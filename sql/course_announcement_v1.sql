-- Course Announcement V1 + minimal user_notification (MySQL 5.7)

CREATE TABLE IF NOT EXISTS course_announcement (
  id INT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  title VARCHAR(200) NOT NULL,
  body_html MEDIUMTEXT NOT NULL,
  author_user_id INT NOT NULL,
  author_name VARCHAR(255) NOT NULL,
  posted_at DATETIME NOT NULL,
  edited_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_course_announcement_course_posted (course_id, posted_at),
  CONSTRAINT fk_course_announcement_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course_announcement_read (
  announcement_id INT NOT NULL,
  user_id INT NOT NULL,
  read_at DATETIME NOT NULL,
  PRIMARY KEY (announcement_id, user_id),
  KEY idx_announcement_read_user (user_id),
  CONSTRAINT fk_announcement_read_announcement FOREIGN KEY (announcement_id)
    REFERENCES course_announcement (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Durable notifications for Announcement (and future event types).
-- No FK on ref_id: announcement delete keeps notification rows (dangling ref allowed).
CREATE TABLE IF NOT EXISTS user_notification (
  id INT NOT NULL AUTO_INCREMENT,
  recipient_user_id INT NOT NULL,
  course_id INT NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  ref_id INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  deep_link VARCHAR(512) NOT NULL,
  created_at DATETIME NOT NULL,
  read_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_recipient_event_ref (recipient_user_id, event_type, ref_id),
  KEY idx_user_notification_recipient_created (recipient_user_id, created_at),
  KEY idx_user_notification_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
