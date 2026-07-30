-- Greenfield user_notification (V1 shape). Use when table does not yet exist.
-- If migrating from course_announcement_v1, use notification_v1.sql instead.

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
  ),
  KEY idx_notification_recipient_created (tenant_id, recipient_user_id, created_at, id),
  KEY idx_notification_recipient_unread (tenant_id, recipient_user_id, read_at),
  KEY idx_user_notification_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
