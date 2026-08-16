-- Phase 1 notification channel tables.
-- Does not modify user_notification.
-- Idempotent: CREATE TABLE IF NOT EXISTS.

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
