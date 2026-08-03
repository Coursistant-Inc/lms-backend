-- admin_identity_phase2_expand.sql
-- Expand-only: additive columns/tables. Safe before backfill.

CREATE TABLE IF NOT EXISTS account_identity (
  id INT NOT NULL AUTO_INCREMENT,
  normalized_email VARCHAR(255) NOT NULL,
  principal_type VARCHAR(16) NOT NULL,
  principal_id INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_account_identity_email (normalized_email),
  UNIQUE KEY uk_account_identity_principal (principal_type, principal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS identity_audit (
  id BIGINT NOT NULL AUTO_INCREMENT,
  actor_id INT NULL,
  actor_role VARCHAR(32) NULL,
  actor_tenant_id INT NULL,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(32) NOT NULL,
  target_id INT NULL,
  target_tenant_id INT NULL,
  before_json JSON NULL,
  after_json JSON NULL,
  reason VARCHAR(512) NULL,
  result VARCHAR(32) NOT NULL,
  ip VARCHAR(64) NULL,
  trace_id VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_identity_audit_target (target_type, target_id),
  KEY idx_identity_audit_actor (actor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS email_outbox (
  id BIGINT NOT NULL AUTO_INCREMENT,
  recipient_email VARCHAR(255) NOT NULL,
  subject VARCHAR(255) NOT NULL,
  body_ciphertext TEXT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  sent_at DATETIME(3) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

ALTER TABLE `user`
  ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' AFTER level,
  ADD COLUMN IF NOT EXISTS auth_version INT NOT NULL DEFAULT 1 AFTER status,
  ADD COLUMN IF NOT EXISTS temporary_password_expires_at DATETIME NULL AFTER must_change_password;

ALTER TABLE admin
  ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' AFTER role,
  ADD COLUMN IF NOT EXISTS auth_version INT NOT NULL DEFAULT 1 AFTER status;

ALTER TABLE tenant
  ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' AFTER timezone,
  ADD COLUMN IF NOT EXISTS security_version INT NOT NULL DEFAULT 1 AFTER status;
