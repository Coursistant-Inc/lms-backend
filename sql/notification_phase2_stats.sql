-- Phase 2 outbox / delivery visibility. Read-only. Safe to run any time.
-- Empty snapshots are reported separately from failures.
-- Zero eligible members at publish time is not a publish failure.
-- course_missing / subject_missing can be historical deletes, not publish bugs.
-- empty_suspect vs current roster can false-positive after later enrollments
-- and false-negative after later drops.

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
    'GROUP_MEMBER_MOVED',
    'ASSIGNMENT_PUBLISHED',
    'ANNOUNCEMENT_POSTED'
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
  'GROUP_MEMBER_MOVED',
  'ASSIGNMENT_PUBLISHED',
  'ANNOUNCEMENT_POSTED'
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
  'GROUP_MEMBER_MOVED',
  'ASSIGNMENT_PUBLISHED',
  'ANNOUNCEMENT_POSTED'
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

-- A. tenant / course / subject alignment. Missing rows are historical deletes.
SELECT
  SUM(CASE WHEN c.id IS NULL THEN 1 ELSE 0 END) AS course_missing,
  SUM(CASE WHEN c.id IS NOT NULL AND o.tenant_id <> c.tenant_id THEN 1 ELSE 0 END) AS tenant_course_mismatch,
  SUM(CASE
        WHEN o.notification_type IN ('WEEK_PUBLISHED') AND w.id IS NULL THEN 1
        WHEN o.notification_type IN ('ASSIGNMENT_PUBLISHED', 'ASSIGNMENT_SCHEDULE_CHANGED') AND a.id IS NULL THEN 1
        WHEN o.notification_type IN ('QUIZ_PUBLISHED', 'QUIZ_SCHEDULE_CHANGED', 'QUIZ_TIME_LIMIT_CHANGED') AND q.id IS NULL THEN 1
        WHEN o.notification_type = 'COURSE_EVENT_CREATED' AND e.id IS NULL THEN 1
        WHEN o.notification_type IN ('GROUP_MEMBER_ADDED', 'GROUP_MEMBER_REMOVED', 'GROUP_MEMBER_MOVED') AND g.id IS NULL THEN 1
        WHEN o.notification_type = 'ANNOUNCEMENT_POSTED' AND n.id IS NULL THEN 1
        ELSE 0
      END) AS subject_missing,
  SUM(CASE
        WHEN w.id IS NOT NULL AND w.course_id <> o.course_id THEN 1
        WHEN a.id IS NOT NULL AND a.course_id <> o.course_id THEN 1
        WHEN q.id IS NOT NULL AND q.course_id <> o.course_id THEN 1
        WHEN e.id IS NOT NULL AND e.course_id <> o.course_id THEN 1
        WHEN g.id IS NOT NULL AND g.course_id <> o.course_id THEN 1
        WHEN n.id IS NOT NULL AND n.course_id <> o.course_id THEN 1
        ELSE 0
      END) AS subject_course_mismatch
FROM notification_event_outbox o
LEFT JOIN course c ON c.id = o.course_id
LEFT JOIN course_week w
  ON w.id = o.subject_id AND o.notification_type = 'WEEK_PUBLISHED'
LEFT JOIN assignment a
  ON a.id = o.subject_id AND o.notification_type IN ('ASSIGNMENT_PUBLISHED', 'ASSIGNMENT_SCHEDULE_CHANGED')
LEFT JOIN quiz q
  ON q.id = o.subject_id AND o.notification_type IN ('QUIZ_PUBLISHED', 'QUIZ_SCHEDULE_CHANGED', 'QUIZ_TIME_LIMIT_CHANGED')
LEFT JOIN course_event e
  ON e.id = o.subject_id AND o.notification_type = 'COURSE_EVENT_CREATED'
LEFT JOIN group_set g
  ON g.id = o.subject_id AND o.notification_type IN ('GROUP_MEMBER_ADDED', 'GROUP_MEMBER_REMOVED', 'GROUP_MEMBER_MOVED')
LEFT JOIN course_announcement n
  ON n.id = o.subject_id AND o.notification_type = 'ANNOUNCEMENT_POSTED'
WHERE o.notification_type IN (
  'WEEK_PUBLISHED',
  'ASSIGNMENT_SCHEDULE_CHANGED',
  'QUIZ_PUBLISHED',
  'QUIZ_SCHEDULE_CHANGED',
  'QUIZ_TIME_LIMIT_CHANGED',
  'COURSE_EVENT_CREATED',
  'GROUP_MEMBER_ADDED',
  'GROUP_MEMBER_REMOVED',
  'GROUP_MEMBER_MOVED',
  'ASSIGNMENT_PUBLISHED',
  'ANNOUNCEMENT_POSTED'
);

-- B. notification type vs subject type matrix.
SELECT
  COUNT(*) AS type_subject_mismatch
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
  'GROUP_MEMBER_MOVED',
  'ASSIGNMENT_PUBLISHED',
  'ANNOUNCEMENT_POSTED'
)
AND NOT (
     (notification_type = 'WEEK_PUBLISHED' AND subject_type = 'WEEK')
  OR (notification_type IN ('ASSIGNMENT_PUBLISHED', 'ASSIGNMENT_SCHEDULE_CHANGED') AND subject_type = 'ASSIGNMENT')
  OR (notification_type IN ('QUIZ_PUBLISHED', 'QUIZ_SCHEDULE_CHANGED', 'QUIZ_TIME_LIMIT_CHANGED') AND subject_type = 'QUIZ')
  OR (notification_type = 'COURSE_EVENT_CREATED' AND subject_type = 'COURSE_EVENT')
  OR (notification_type IN ('GROUP_MEMBER_ADDED', 'GROUP_MEMBER_REMOVED', 'GROUP_MEMBER_MOVED') AND subject_type = 'GROUP_SET')
  OR (notification_type = 'ANNOUNCEMENT_POSTED' AND subject_type = 'ANNOUNCEMENT')
);

-- C. empty snapshot classification. Current roster is a hint, not a time-travel snapshot.
SELECT
  classified.empty_class,
  COUNT(*) AS empty_class_count
FROM (
  SELECT
    o.id,
    CASE
      WHEN COUNT(r.id) > 0 THEN 'has_recipients'
      WHEN o.notification_type IN ('GROUP_MEMBER_ADDED', 'GROUP_MEMBER_REMOVED', 'GROUP_MEMBER_MOVED')
           AND o.event_key LIKE '%:target' THEN 'empty_suspect'
      WHEN o.notification_type IN ('GROUP_MEMBER_ADDED', 'GROUP_MEMBER_REMOVED', 'GROUP_MEMBER_MOVED')
           AND (o.event_key LIKE '%:members'
             OR o.event_key LIKE '%:old-members'
             OR o.event_key LIKE '%:new-members') THEN 'empty_suspect'
      WHEN o.notification_type IN (
             'WEEK_PUBLISHED',
             'ASSIGNMENT_PUBLISHED',
             'ASSIGNMENT_SCHEDULE_CHANGED',
             'QUIZ_PUBLISHED',
             'QUIZ_SCHEDULE_CHANGED',
             'QUIZ_TIME_LIMIT_CHANGED'
           )
           AND (SELECT COUNT(*) FROM enrollment e
                WHERE e.course_id = o.course_id AND e.active = 1 AND e.course_role = 'Student') = 0
           THEN 'empty_expected'
      WHEN o.notification_type IN (
             'WEEK_PUBLISHED',
             'ASSIGNMENT_PUBLISHED',
             'ASSIGNMENT_SCHEDULE_CHANGED',
             'QUIZ_PUBLISHED',
             'QUIZ_SCHEDULE_CHANGED',
             'QUIZ_TIME_LIMIT_CHANGED'
           ) THEN 'empty_suspect'
      WHEN o.notification_type IN ('COURSE_EVENT_CREATED', 'ANNOUNCEMENT_POSTED')
           AND (SELECT COUNT(*) FROM enrollment e
                WHERE e.course_id = o.course_id AND e.active = 1) = 0
           THEN 'empty_expected'
      WHEN o.notification_type IN ('COURSE_EVENT_CREATED', 'ANNOUNCEMENT_POSTED') THEN 'empty_suspect'
      ELSE 'empty_unclassified'
    END AS empty_class
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
    'GROUP_MEMBER_MOVED',
    'ASSIGNMENT_PUBLISHED',
    'ANNOUNCEMENT_POSTED'
  )
  GROUP BY o.id, o.notification_type, o.event_key, o.course_id
) classified
GROUP BY classified.empty_class
ORDER BY classified.empty_class;
