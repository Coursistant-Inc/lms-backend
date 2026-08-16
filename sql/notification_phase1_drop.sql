-- DANGER: drops phase-1 notification tables and any unsent backlog.
-- Run notification_phase1_stats.sql first and archive results.
-- Safe rollback does NOT use this file; leave the tables in place.

DROP TABLE IF EXISTS notification_event_recipient;
DROP TABLE IF EXISTS notification_delivery;
DROP TABLE IF EXISTS notification_digest_email;
DROP TABLE IF EXISTS notification_event_outbox;
