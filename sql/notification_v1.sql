-- Notification V1: migrate existing user_notification (from course_announcement_v1.sql).
-- MySQL 5.7+. One-shot migration; do not re-run after success without review.
--
-- Gate checks (run manually after step 3):
--   SELECT COUNT(*) AS orphan_remaining FROM user_notification WHERE tenant_id IS NULL;
--   -- must be 0 before NOT NULL
--   SHOW INDEX FROM user_notification WHERE Key_name = 'uk_notification_dedupe';

-- ---------------------------------------------------------------------------
-- 1) Add new columns (nullable first)
-- ---------------------------------------------------------------------------
ALTER TABLE user_notification
  ADD COLUMN tenant_id INT NULL AFTER id,
  ADD COLUMN notification_type VARCHAR(64) NULL AFTER course_id,
  ADD COLUMN message VARCHAR(512) NULL AFTER notification_type,
  ADD COLUMN subject_type VARCHAR(64) NULL AFTER message,
  ADD COLUMN subject_id INT NULL AFTER subject_type,
  ADD COLUMN event_key VARCHAR(128) NULL AFTER subject_id;

-- ---------------------------------------------------------------------------
-- 2) Backfill from course + legacy columns
-- ---------------------------------------------------------------------------
UPDATE user_notification n
INNER JOIN course c ON c.id = n.course_id
SET n.tenant_id = c.tenant_id
WHERE n.tenant_id IS NULL;

UPDATE user_notification
SET notification_type = event_type
WHERE notification_type IS NULL AND event_type IS NOT NULL;

UPDATE user_notification
SET message = LEFT(title, 512)
WHERE message IS NULL AND title IS NOT NULL;

UPDATE user_notification
SET subject_type = 'ANNOUNCEMENT'
WHERE subject_type IS NULL;

UPDATE user_notification
SET subject_id = ref_id
WHERE subject_id IS NULL AND ref_id IS NOT NULL;

UPDATE user_notification
SET event_key = CONCAT('legacy:', id)
WHERE event_key IS NULL;

-- ---------------------------------------------------------------------------
-- 3) Orphan rows (course gone / no FK) — DELETE; never guess user.tenant_id
-- Log count first:
--   SELECT COUNT(*) AS orphan_count FROM user_notification WHERE tenant_id IS NULL;
DELETE FROM user_notification WHERE tenant_id IS NULL;

-- Hard gate: SELECT COUNT(*) FROM user_notification WHERE tenant_id IS NULL;  -- must be 0

-- ---------------------------------------------------------------------------
-- 4) Deduplicate before new unique key (keep smallest id)
-- ---------------------------------------------------------------------------
DELETE n1 FROM user_notification n1
INNER JOIN user_notification n2
  ON n1.tenant_id = n2.tenant_id
 AND n1.recipient_user_id = n2.recipient_user_id
 AND n1.notification_type = n2.notification_type
 AND n1.subject_type = n2.subject_type
 AND n1.subject_id = n2.subject_id
 AND n1.event_key = n2.event_key
 AND n1.id > n2.id;

-- ---------------------------------------------------------------------------
-- 5) Enforce NOT NULL
-- ---------------------------------------------------------------------------
ALTER TABLE user_notification
  MODIFY COLUMN tenant_id INT NOT NULL,
  MODIFY COLUMN notification_type VARCHAR(64) NOT NULL,
  MODIFY COLUMN message VARCHAR(512) NOT NULL,
  MODIFY COLUMN subject_type VARCHAR(64) NOT NULL,
  MODIFY COLUMN subject_id INT NOT NULL,
  MODIFY COLUMN event_key VARCHAR(128) NOT NULL;

-- ---------------------------------------------------------------------------
-- 6) Replace unique key + indexes
-- ---------------------------------------------------------------------------
ALTER TABLE user_notification DROP INDEX uk_recipient_event_ref;

-- Drop old recipient-created index if present (replaced by tenant-scoped index)
-- Ignore error if name differs on some installs:
ALTER TABLE user_notification DROP INDEX idx_user_notification_recipient_created;

ALTER TABLE user_notification
  ADD UNIQUE KEY uk_notification_dedupe (
    tenant_id, recipient_user_id, notification_type, subject_type, subject_id, event_key
  ),
  ADD KEY idx_notification_recipient_created (tenant_id, recipient_user_id, created_at, id),
  ADD KEY idx_notification_recipient_unread (tenant_id, recipient_user_id, read_at);

-- ---------------------------------------------------------------------------
-- 7) Drop legacy columns (event_type / ref_id / title)
-- Idempotent: full leftover, partial leftover (1–2 columns), or already dropped.
-- Run with mysql CLI (DELIMITER required). Do not execute via JDBC Statement/ScriptUtils.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS drop_user_notification_legacy_columns;
DELIMITER //
CREATE PROCEDURE drop_user_notification_legacy_columns()
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user_notification'
      AND column_name = 'event_type'
  ) THEN
    ALTER TABLE user_notification DROP COLUMN event_type;
  END IF;
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user_notification'
      AND column_name = 'ref_id'
  ) THEN
    ALTER TABLE user_notification DROP COLUMN ref_id;
  END IF;
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user_notification'
      AND column_name = 'title'
  ) THEN
    ALTER TABLE user_notification DROP COLUMN title;
  END IF;
END //
DELIMITER ;
CALL drop_user_notification_legacy_columns();
DROP PROCEDURE IF EXISTS drop_user_notification_legacy_columns;
