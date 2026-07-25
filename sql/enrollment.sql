-- Enrollment + EnrollmentAuditLog for lms_v2

CREATE TABLE IF NOT EXISTS enrollment (
  id INT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  user_id INT NOT NULL,
  course_role ENUM('Student', 'TA', 'Instructor') NOT NULL,
  can_grade TINYINT(1) NOT NULL DEFAULT 0,
  can_post_announcements TINYINT(1) NOT NULL DEFAULT 0,
  can_manage_groups TINYINT(1) NOT NULL DEFAULT 0,
  can_manage_course_events TINYINT(1) NOT NULL DEFAULT 0,
  active TINYINT(1) NOT NULL DEFAULT 1,
  enrolled_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  instructor_course_id INT GENERATED ALWAYS AS (IF(course_role = 'Instructor', course_id, NULL)) STORED,
  PRIMARY KEY (id),
  UNIQUE KEY uk_enrollment_course_user (course_id, user_id),
  UNIQUE KEY uk_enrollment_one_instructor (instructor_course_id),
  KEY idx_enrollment_user (user_id),
  KEY idx_enrollment_course (course_id),
  CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES Course (id) ON DELETE RESTRICT,
  CONSTRAINT fk_enrollment_user FOREIGN KEY (user_id) REFERENCES User (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS enrollment_audit_log (
  id INT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  target_user_id INT NOT NULL,
  actor_type ENUM('USER', 'ADMIN') NOT NULL,
  actor_id INT NOT NULL,
  action VARCHAR(64) NOT NULL,
  detail_json TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_enrollment_audit_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
