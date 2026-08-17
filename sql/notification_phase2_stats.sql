-- Phase 2 outbox / delivery visibility. Empty snapshots are reported separately from failures.
-- Zero eligible members at publish time is not a publish failure.

SELECT
  notification_type,
  status,
  COUNT(*) AS outbox_count
FROM notification_event_outbox
WHERE notification_type IN (
  'WEEK_PUBLISHED',
  'ASSIGNMENT_SCHEDULE_CHANGED',
  'QUIZ_PUBLISHED',
  'QUIZ_SCHEDULE_CHANGED',
  'QUIZ_TIME_LIMIT_CHANGED',
  'COURSE_EVENT_CREATED',
  'GROUP_MEMBER_ADDED',
  'GROUP_MEMBER_REMOVED',
  'GROUP_MEMBER_MOVED'
)
GROUP BY notification_type, status
ORDER BY notification_type, status;

SELECT
  o.notification_type,
  COUNT(*) AS empty_snapshot_count
FROM notification_event_outbox o
LEFT JOIN notification_event_recipient r ON r.outbox_id = o.id
WHERE o.notification_type IN (
  'WEEK_PUBLISHED',
  'ASSIGNMENT_SCHEDULE_CHANGED',
  'QUIZ_PUBLISHED',
  'QUIZ_SCHEDULE_CHANGED',
  'QUIZ_TIME_LIMIT_CHANGED',
  'COURSE_EVENT_CREATED',
  'GROUP_MEMBER_ADDED',
  'GROUP_MEMBER_REMOVED',
  'GROUP_MEMBER_MOVED'
)
GROUP BY o.id, o.notification_type
HAVING COUNT(r.id) = 0;

SELECT
  d.notification_type,
  d.channel,
  d.status,
  COUNT(*) AS delivery_count
FROM notification_delivery d
WHERE d.notification_type IN (
  'WEEK_PUBLISHED',
  'ASSIGNMENT_SCHEDULE_CHANGED',
  'QUIZ_PUBLISHED',
  'QUIZ_SCHEDULE_CHANGED',
  'QUIZ_TIME_LIMIT_CHANGED',
  'COURSE_EVENT_CREATED',
  'GROUP_MEMBER_ADDED',
  'GROUP_MEMBER_REMOVED',
  'GROUP_MEMBER_MOVED'
)
GROUP BY d.notification_type, d.channel, d.status
ORDER BY d.notification_type, d.channel, d.status;

SELECT
  COUNT(*) AS unexpected_immediate_email
FROM notification_delivery
WHERE notification_type IN (
  'WEEK_PUBLISHED',
  'ASSIGNMENT_SCHEDULE_CHANGED',
  'QUIZ_PUBLISHED',
  'QUIZ_SCHEDULE_CHANGED',
  'QUIZ_TIME_LIMIT_CHANGED',
  'COURSE_EVENT_CREATED',
  'GROUP_MEMBER_ADDED',
  'GROUP_MEMBER_REMOVED',
  'GROUP_MEMBER_MOVED'
)
AND channel = 'IMMEDIATE_EMAIL';
