-- Auth IT greenfield schema (MySQL 8). Applied by AuthIntegrationTestBase.
-- Image tag: mysql:8.0.36 (aligned with mysql-connector-j 8.0.33 / MySQL 8.0 family)

CREATE TABLE IF NOT EXISTS tenant (
  id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  timezone VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  security_version INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
  id INT NOT NULL AUTO_INCREMENT,
  tenant_id INT NULL,
  username VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  name VARCHAR(255) NULL,
  email VARCHAR(255) NOT NULL,
  avatar VARCHAR(512) NULL,
  role VARCHAR(32) NOT NULL,
  level VARCHAR(32) NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  auth_version INT NOT NULL DEFAULT 1,
  must_change_password TINYINT(1) NOT NULL DEFAULT 0,
  email_notifications TINYINT(1) NOT NULL DEFAULT 1,
  temporary_password_expires_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_email (email),
  UNIQUE KEY uk_user_username (username),
  KEY idx_user_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin (
  id INT NOT NULL AUTO_INCREMENT,
  username VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  name VARCHAR(255) NULL,
  phone VARCHAR(64) NULL,
  email VARCHAR(255) NOT NULL,
  avatar VARCHAR(512) NULL,
  role VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  auth_version INT NOT NULL DEFAULT 1,
  invitation VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_admin_email (email),
  UNIQUE KEY uk_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE IF NOT EXISTS refresh_tokens (
  id INT NOT NULL AUTO_INCREMENT,
  session_id VARCHAR(64) NULL,
  user_id INT NOT NULL,
  token VARCHAR(128) NOT NULL,
  previous_token VARCHAR(128) NULL,
  previous_valid_until DATETIME NULL,
  ip_address VARCHAR(64) NULL,
  user_agent VARCHAR(512) NULL,
  expire_time DATETIME NOT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  role VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_refresh_tokens_session_id (session_id),
  KEY idx_refresh_tokens_user (user_id),
  KEY idx_refresh_tokens_token (token),
  KEY idx_refresh_tokens_previous (previous_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO tenant (id, name, timezone, status, security_version)
VALUES (1, 'Default', 'America/Los_Angeles', 'ACTIVE', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), timezone = VALUES(timezone);

INSERT INTO tenant (id, name, timezone, status, security_version)
VALUES (2, 'Tenant Two', 'America/New_York', 'ACTIVE', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO tenant (id, name, timezone, status, security_version)
VALUES (3, 'Disabled Tenant', 'UTC', 'DISABLED', 1)
ON DUPLICATE KEY UPDATE status = 'DISABLED';
