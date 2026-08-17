-- Phase 2 notification version columns.
-- Additive and idempotent. Compatible with MySQL 8.0; ALTER is guarded via information_schema.
-- Safe to re-run. Do not DROP columns here.
-- Rollout: stop all old Relays before deploying this binary. New NotificationType values
-- are not backward compatible. Do not mix new producers with old consumers.
-- Rollback: keep these columns; never DROP them online. Drain new-type outbox with the
-- phase-2 binary before starting an old Relay with outbox.enabled=true.

SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'assignment' AND column_name = 'publication_version'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE assignment ADD COLUMN publication_version INT NOT NULL DEFAULT 0',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'assignment' AND column_name = 'schedule_version'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE assignment ADD COLUMN schedule_version INT NOT NULL DEFAULT 0',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'quiz' AND column_name = 'publication_version'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE quiz ADD COLUMN publication_version INT NOT NULL DEFAULT 0',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'course_week' AND column_name = 'publication_version'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE course_week ADD COLUMN publication_version INT NOT NULL DEFAULT 0',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
