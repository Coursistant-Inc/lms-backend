-- Operational stats for phase-1 notifications. Safe to run any time.

SELECT notification_type, status, COUNT(*) AS cnt
FROM notification_event_outbox
GROUP BY notification_type, status
ORDER BY notification_type, status;

SELECT channel, status, failure_category, COUNT(*) AS cnt
FROM notification_delivery
GROUP BY channel, status, failure_category
ORDER BY channel, status, failure_category;

SELECT status, COUNT(*) AS cnt
FROM notification_digest_email
GROUP BY status
ORDER BY status;

SELECT COUNT(*) AS outbox_backlog
FROM notification_event_outbox
WHERE status IN ('PENDING', 'FAILED_RETRYABLE', 'PROCESSING');

SELECT COUNT(*) AS delivery_orphans
FROM notification_delivery
WHERE status = 'PROCESSING' AND (lease_until IS NULL OR lease_until < UTC_TIMESTAMP(3));

SELECT COUNT(*) AS unknown_outcome_rows
FROM notification_delivery
WHERE send_attempted_at IS NOT NULL AND sent_at IS NULL AND status = 'PROCESSING';

SELECT COUNT(*) AS stale_digest_pending
FROM notification_delivery
WHERE channel = 'DAILY_DIGEST' AND status = 'PENDING' AND digest_date < CURRENT_DATE;
