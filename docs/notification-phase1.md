# Notification Phase 1 — configuration and operations

Phase 1 delivers In-app, Immediate Email, and Daily Digest through a single chain:
**Publisher → outbox → Relay → Fanout → Immediate Email / Daily Digest**.
`user_notification` (student `/v2/me/notifications`) is unchanged. Delivery semantics are **at-least-once**.

`lms.notification.outbox.enabled` only pauses **Relay consumption** (poll and after-commit fast-path).
The Publisher **always** writes a PENDING outbox row, even when the switch is `false`.

## Configuration

```yaml
lms:
  notification:
    outbox:
      enabled: true
      poll-ms: 5000
      batch-size: 100
      max-attempts: 8
      recipient-insert-chunk: 500
      lease-seconds: 120
    digest:
      default-time-zone: America/Los_Angeles
      batch-size: 200
      lease-seconds: 120
      max-attempts: 5
    email:
      enabled: true
      provider: log          # log | smtp
      from-address: ${MAIL_USERNAME:do.not.reply@coursistant.com}
      from-name: xLearn
      base-url: ${APP_BASE_URL:https://dev.xlearnedu.com}
      max-attempts: 5
      backoff-base-seconds: 2
      poll-ms: 5000
      batch-size: 50
      lease-seconds: 120

spring:
  mail:
    properties:
      mail:
        smtp:
          connectiontimeout: 10000
          timeout: 20000
          writetimeout: 20000
```

Secrets stay in `.env`. Do not log them.

| Variable | Purpose |
| --- | --- |
| `MAIL_USERNAME` | SMTP username and default `from-address` |
| `MAIL_PASSWORD` | SMTP password |
| `LMS_NOTIFICATION_EMAIL_PROVIDER` | `log` (default, writes `DRY_RUN`) or `smtp` |
| `APP_BASE_URL` | Prefix for email deep links |

Dev default `provider=log` records DRY_RUN and never talks to SMTP.
Production: `LMS_NOTIFICATION_EMAIL_PROVIDER=smtp`.

## Deploy

Keep `lms.notification.outbox.enabled=true` on every instance while old and new binaries can both be running.
Old builds treat `enabled=false` as “skip outbox and dispatch after commit”; new builds treat it as “write outbox but pause Relay”. Mixing those meanings loses or double-sends events.

1. Run `sql/notification_phase1.sql`.
2. Run `sql/notification_phase1_gate_check.sql` and confirm table/index counts are 1. `digest_terminal_parent_processing_children` and `overdue_collecting` must be 0.
3. Start with `provider=log` so new email rows land as `DRY_RUN`.
4. Deploy the new binary. Confirm old instances have exited before changing the outbox switch.
5. After cutover, `outbox.enabled=false` may be used briefly to inspect backlog; turn it back on to resume Relay.
6. Switch `provider=smtp`. Only mail created after the switch is sent. `DRY_RUN` stays terminal; there is no requeue endpoint.
7. Optional canary: `POST /v2/admin/notifications/digest/run` with a required `digestDate` and optional `tenantId`.

## Rollback (do not drop tables)

1. `provider=log` or `lms.notification.email.enabled=false` — stop real SMTP.
   Closing `email.enabled` skips the Immediate worker and Digest Phase B (`sendOne`); frozen envelopes stay `PENDING` and send after the switch is turned back on. Digest Phase A collect still runs.
2. **Set `lms.notification.outbox.enabled=true` before starting the old binary.** Old code with `enabled=false` bypasses outbox and dispatches after commit.
3. Roll back the binary. Leave the four notification tables in place.
4. Old code will keep consuming PENDING outbox rows. To halt consumption completely, stop the process; do not use `outbox.enabled=false` on the old binary.

`sql/notification_phase1_drop.sql` is only for an explicit wipe after exporting `sql/notification_phase1_stats.sql`.

## At-least-once

SMTP has no idempotency key. If the process dies after the provider accepts a message and before `SENT` is written, recovery may send that message **once more**. A second unknown outcome becomes `FAILED_PERMANENT(UNKNOWN_OUTCOME)`.

Normal paths (concurrency, restart, lease reclaim) do not duplicate.

## Admin

Requires `authzService.requireSystemAdmin` and writes `IdentityAuditService`.

- `POST /v2/admin/notifications/digest/run` — body must include `digestDate`; optional `tenantId` scopes Phase A and Phase B. There is no delivery retry or DRY_RUN requeue HTTP API.

## Troubleshooting

Look for structured log events: `stale_claim`, `orphan_reclaimed`, `unknown_outcome`, `recipient_resolution_failed`, `digest_phase_a_failed`.
Correlate with `eventId`, `tenantId`, `eventType`, `channel`, `status`, `recipientUserId`. Emails are masked; bodies, secrets, tokens, and scores are not logged.
