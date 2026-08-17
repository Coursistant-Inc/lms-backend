# Notification API

In-app notification inbox for the authenticated user, plus a SYSTEM_ADMIN digest trigger.

Formal contract: [`notification.openapi.yaml`](notification.openapi.yaml).
Base URL: `/api` (matches `server.servlet.context-path`).

## Auth

All endpoints require HTTP Bearer JWT (`Authorization: Bearer <accessToken>`).

| Audience | Endpoints |
|----------|-----------|
| Any authenticated user with a tenant | `GET/PATCH /v2/me/notifications*` |
| `SYSTEM_ADMIN` only | `POST /v2/admin/notifications/digest/run` |

Mutating endpoints (`PATCH` mark-read, `POST` digest) require header `Idempotency-Key` (`A-Za-z0-9_-`, max 128). Missing or invalid keys return `400` `IDEMPOTENCY_KEY_REQUIRED` / `IDEMPOTENCY_KEY_INVALID`.

Envelope: `ApiResponse<T>` with `status`, `code`, `data`, `message`, `timestamp`. Success `code` is `SUCCESS`.

## User endpoints

### `GET /v2/me/notifications`

`operationId`: `meNotificationList`

Paged inbox for the caller (newest first).

| Query | Default | Notes |
|-------|---------|-------|
| `page` | `1` | Values `< 1` become `1` |
| `size` | `20` | Values `< 1` become `20`; cap `100` |

`data`:

```json
{
  "items": [ { "notificationId": 42, "availability": "AVAILABLE" } ],
  "page": 1,
  "size": 20,
  "total": 1
}
```

Main errors: `401 UNAUTHORIZED`, `404 USER_NOT_FOUND`, `400 BAD_REQUEST` (user has no tenant).

### `GET /v2/me/notifications/unread-count`

`operationId`: `meNotificationUnreadCount`

`data.unreadCount` is the number of rows with `readAt == null`.

### `PATCH /v2/me/notifications/{notificationId}/read`

`operationId`: `meNotificationMarkRead`

Marks one inbox row read. Already-read rows succeed with no change. A notification that is not owned by the caller returns `404 NOT_FOUND` (no cross-user leak).

Requires `Idempotency-Key`.

### `PATCH /v2/me/notifications/read-all`

`operationId`: `meNotificationMarkAllRead`

Marks every unread row for the caller as read. Returns `data.unreadCount = 0`.

Requires `Idempotency-Key`.

## Admin endpoint

### `POST /v2/admin/notifications/digest/run`

`operationId`: `adminNotificationDigestRun`

Manually run the daily digest email job.

Request body (`DigestRunRequest`):

| Field | Required | Notes |
|-------|----------|-------|
| `digestDate` | yes | ISO date, e.g. `2026-08-17` |
| `tenantId` | no | Omit / `null` to run all tenants |

Main errors: `403 FORBIDDEN` (not SYSTEM_ADMIN), `400 PARAM_MISSING` (`digestDate` missing).

Writes audit action `NOTIFICATION_DIGEST_RUN`.

## `NotificationResponse` fields

| Field | Meaning |
|-------|---------|
| `notificationId` | Inbox row id |
| `tenantId` | Tenant of the recipient |
| `recipientUserId` | Always the caller |
| `courseId` / `courseCode` | Course context when present |
| `notificationType` | Stable enum (see below) |
| `message` | Student-facing text. Never includes numeric scores |
| `subjectType` / `subjectId` | Subject used for availability + navigation |
| `deepLink` | Frontend path, e.g. `/courses/7/assignments/12` |
| `createdAt` | UTC |
| `readAt` | UTC; `null` = unread |
| `availability` | `AVAILABLE` or `NO_LONGER_AVAILABLE` |

### `availability`

Computed at read time for the current viewer:

- `AVAILABLE` — the subject still exists, the viewer is enrolled, and (for assignment/quiz/week) it is Published or the viewer is staff. Submission rows are available to staff, the owner, or current group members.
- `NO_LONGER_AVAILABLE` — dropped enrollment, unpublished/deleted subject, or a grade-correction subject (`ASSIGNMENT_GRADE` / `QUIZ_GRADE`) that has no live deep-link target.

Clients should disable or hide the deep link when `availability` is `NO_LONGER_AVAILABLE`.

### `notificationType`

`ANNOUNCEMENT_POSTED`, `ASSIGNMENT_PUBLISHED`, `ASSIGNMENT_SUBMISSION_RECEIVED`, `ASSIGNMENT_GRADE_RELEASED`, `QUIZ_GRADE_RELEASED`, `ASSIGNMENT_GRADE_CORRECTED`, `QUIZ_GRADE_CORRECTED`, `WEEK_PUBLISHED`, `ASSIGNMENT_SCHEDULE_CHANGED`, `QUIZ_PUBLISHED`, `QUIZ_SCHEDULE_CHANGED`, `QUIZ_TIME_LIMIT_CHANGED`, `COURSE_EVENT_CREATED`, `GROUP_MEMBER_ADDED`, `GROUP_MEMBER_REMOVED`, `GROUP_MEMBER_MOVED`.

### `subjectType`

`ANNOUNCEMENT`, `ASSIGNMENT`, `QUIZ`, `ASSIGNMENT_GRADE`, `QUIZ_GRADE`, `ASSIGNMENT_SUBMISSION`, `WEEK`, `COURSE_EVENT`, `GROUP_SET`.

## Export / verify

```powershell
./scripts/export-openapi.ps1 -Module notification
./scripts/verify-openapi.ps1 -Module notification
```
