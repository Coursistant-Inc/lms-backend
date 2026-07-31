-- Drop legacy columns left after notification_v1 migration (MySQL 5.7+).
-- Idempotent: supports full leftover, partial leftover (1–2 columns), or already dropped.
-- Run with mysql CLI (DELIMITER required). Do not execute via JDBC Statement/ScriptUtils.

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
