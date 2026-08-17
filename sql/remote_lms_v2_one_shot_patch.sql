-- One-shot patch for remote lms_v2 vs 2026-08-17 structure dump (lms_v2111.sql).
-- MySQL 8.0+. Idempotent. Additive except dropping leftover user_notification
-- legacy columns (event_type / ref_id / title) that current code no longer writes.
--
-- Does NOT recreate existing tables. Does NOT delete business data.
-- Safe to re-run. No DELIMITER — works in Navicat / mysql CLI.
--
-- Apply on the live schema (lms_v2), then cut over to the new JAR:
--   stop old instances / Relays, deploy the new binary in one shot.
-- Backup the database first.

-- ===========================================================================
-- 1) Notification phase 1 channel tables
-- ===========================================================================

CREATE TABLE IF NOT EXISTS notification_event_outbox (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id CHAR(36) NOT NULL,
  tenant_id INT NOT NULL,
  course_id INT NOT NULL,
  notification_type VARCHAR(64) NOT NULL,
  subject_type VARCHAR(64) NOT NULL,
  subject_id INT NOT NULL,
  event_key VARCHAR(128) NOT NULL,
  actor_user_id INT NULL,
  message VARCHAR(512) NOT NULL,
  deep_link VARCHAR(512) NOT NULL,
  occurred_at DATETIME(3) NOT NULL,
  recipient_mode VARCHAR(32) NOT NULL,
  template_vars_json VARCHAR(2000) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  attempt_count INT NOT NULL DEFAULT 0,
  next_attempt_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  lease_until DATETIME(3) NULL,
  claim_token CHAR(36) NULL,
  last_error VARCHAR(512) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_outbox_event (tenant_id, notification_type, subject_type, subject_id, event_key),
  UNIQUE KEY uk_outbox_event_id (event_id),
  KEY idx_outbox_claim (status, next_attempt_at, lease_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notification_event_recipient (
  id BIGINT NOT NULL AUTO_INCREMENT,
  outbox_id BIGINT NOT NULL,
  recipient_user_id INT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_event_recipient (outbox_id, recipient_user_id),
  KEY idx_event_recipient_outbox (outbox_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notification_delivery (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id CHAR(36) NOT NULL,
  tenant_id INT NOT NULL,
  recipient_user_id INT NOT NULL,
  course_id INT NOT NULL,
  notification_type VARCHAR(64) NOT NULL,
  subject_type VARCHAR(64) NOT NULL,
  subject_id INT NOT NULL,
  event_key VARCHAR(128) NOT NULL,
  channel VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  message VARCHAR(512) NOT NULL,
  deep_link VARCHAR(512) NOT NULL,
  template_vars_json VARCHAR(2000) NULL,
  occurred_at DATETIME(3) NOT NULL,
  digest_date DATE NULL,
  digest_email_id BIGINT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  next_attempt_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  lease_until DATETIME(3) NULL,
  claim_token CHAR(36) NULL,
  send_attempted_at DATETIME(3) NULL,
  unknown_outcome_count INT NOT NULL DEFAULT 0,
  failure_category VARCHAR(64) NULL,
  last_error VARCHAR(512) NULL,
  provider_message_id VARCHAR(255) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  sent_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_delivery_dedupe (
    tenant_id, recipient_user_id, notification_type, subject_type, subject_id, event_key, channel
  ),
  KEY idx_delivery_claim (channel, status, next_attempt_at, lease_until),
  KEY idx_delivery_digest (channel, status, digest_date, tenant_id, recipient_user_id),
  KEY idx_delivery_digest_email (digest_email_id),
  KEY idx_delivery_event (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notification_digest_email (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id INT NOT NULL,
  recipient_user_id INT NOT NULL,
  digest_date DATE NOT NULL,
  status VARCHAR(32) NOT NULL,
  item_count INT NOT NULL DEFAULT 0,
  attempt_count INT NOT NULL DEFAULT 0,
  next_attempt_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  lease_until DATETIME(3) NULL,
  claim_token CHAR(36) NULL,
  send_attempted_at DATETIME(3) NULL,
  unknown_outcome_count INT NOT NULL DEFAULT 0,
  failure_category VARCHAR(64) NULL,
  last_error VARCHAR(512) NULL,
  provider_message_id VARCHAR(255) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  sent_at DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_digest_email (tenant_id, recipient_user_id, digest_date),
  KEY idx_digest_email_claim (status, next_attempt_at, lease_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================================================================
-- 2) Notification phase 2 version columns (keep forever; never DROP online)
-- ===========================================================================

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

-- ===========================================================================
-- 3) SYSTEM_ADMIN grade-correction audit
-- ===========================================================================

CREATE TABLE IF NOT EXISTS grade_correction_audit (
  id BIGINT NOT NULL AUTO_INCREMENT,
  actor_id INT NOT NULL,
  assignment_id INT NULL,
  quiz_id INT NULL,
  student_user_id INT NOT NULL,
  course_id INT NULL,
  tenant_id INT NULL,
  reason VARCHAR(1024) NOT NULL,
  before_json JSON NULL,
  after_json JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================================================================
-- 4) Drop leftover user_notification V1 legacy columns
--    Current mapper no longer reads/writes event_type / ref_id / title.
--    Do not re-run notification_v1.sql ADD COLUMN.
-- ===========================================================================

SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user_notification' AND column_name = 'event_type'
);
SET @sql := IF(@exists > 0,
  'ALTER TABLE user_notification DROP COLUMN event_type',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user_notification' AND column_name = 'ref_id'
);
SET @sql := IF(@exists > 0,
  'ALTER TABLE user_notification DROP COLUMN ref_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user_notification' AND column_name = 'title'
);
SET @sql := IF(@exists > 0,
  'ALTER TABLE user_notification DROP COLUMN title',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===========================================================================
-- 5) Gate: every column below must be 1, every *_invalid / leftover must be 0
-- ===========================================================================

SELECT
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'notification_event_outbox') AS outbox_table,
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'notification_event_recipient') AS recipient_table,
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'notification_delivery') AS delivery_table,
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'notification_digest_email') AS digest_email_table,
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'grade_correction_audit') AS grade_correction_audit_table,
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'assignment'
      AND column_name = 'publication_version' AND is_nullable = 'NO') AS assignment_publication_version,
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'assignment'
      AND column_name = 'schedule_version' AND is_nullable = 'NO') AS assignment_schedule_version,
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'quiz'
      AND column_name = 'publication_version' AND is_nullable = 'NO') AS quiz_publication_version,
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'course_week'
      AND column_name = 'publication_version' AND is_nullable = 'NO') AS course_week_publication_version,
  (SELECT COUNT(*) FROM assignment
    WHERE publication_version IS NULL OR publication_version < 0
       OR schedule_version IS NULL OR schedule_version < 0) AS assignment_invalid_versions,
  (SELECT COUNT(*) FROM quiz
    WHERE publication_version IS NULL OR publication_version < 0) AS quiz_invalid_versions,
  (SELECT COUNT(*) FROM course_week
    WHERE publication_version IS NULL OR publication_version < 0) AS course_week_invalid_versions,
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user_notification'
      AND column_name IN ('event_type', 'ref_id', 'title')) AS legacy_columns_remaining;
