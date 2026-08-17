-- Minimal schema for notification Spring ITs. No foreign keys so tests can wipe freely.
SET NAMES utf8mb4;

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
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_email (email),
  UNIQUE KEY uk_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course (
  id INT NOT NULL AUTO_INCREMENT,
  tenant_id INT NOT NULL,
  course_code VARCHAR(32) NOT NULL,
  title VARCHAR(255) NOT NULL,
  term_start_date DATE NOT NULL,
  term_end_date DATE NOT NULL,
  description TEXT NULL,
  location VARCHAR(255) NULL,
  instructor_id INT NOT NULL,
  state VARCHAR(16) NOT NULL DEFAULT 'Active',
  archived_at DATETIME NULL,
  archived_by_actor_type VARCHAR(16) NULL,
  archived_by_actor_id INT NULL,
  creator_id INT NOT NULL,
  creator_actor_type VARCHAR(16) NULL,
  creator_actor_id INT NULL,
  creator_role VARCHAR(32) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS enrollment (
  id INT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  user_id INT NOT NULL,
  course_role VARCHAR(16) NOT NULL,
  can_grade TINYINT(1) NOT NULL DEFAULT 0,
  can_post_announcements TINYINT(1) NOT NULL DEFAULT 0,
  can_manage_groups TINYINT(1) NOT NULL DEFAULT 0,
  can_manage_course_events TINYINT(1) NOT NULL DEFAULT 0,
  active TINYINT(1) NOT NULL DEFAULT 1,
  assignment_submit_frozen TINYINT(1) NOT NULL DEFAULT 0,
  enrolled_at DATETIME NOT NULL,
  withdrawn_at DATETIME NULL,
  withdrawn_by_actor_type VARCHAR(16) NULL,
  withdrawn_by_actor_id INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_notification (
  id INT NOT NULL AUTO_INCREMENT,
  tenant_id INT NOT NULL,
  recipient_user_id INT NOT NULL,
  course_id INT NOT NULL,
  notification_type VARCHAR(64) NOT NULL,
  message VARCHAR(512) NOT NULL,
  subject_type VARCHAR(64) NOT NULL,
  subject_id INT NOT NULL,
  event_key VARCHAR(128) NOT NULL,
  deep_link VARCHAR(512) NOT NULL,
  created_at DATETIME NOT NULL,
  read_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_dedupe (
    tenant_id, recipient_user_id, notification_type, subject_type, subject_id, event_key
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
