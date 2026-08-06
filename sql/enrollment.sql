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
  assignment_submit_frozen TINYINT(1) NOT NULL DEFAULT 0,
  enrolled_at DATETIME NOT NULL,
  -- Part 3: soft withdraw metadata (see sql/course_part3_expand.sql). Legacy inactive may leave these NULL.
  withdrawn_at DATETIME NULL,
  withdrawn_by_actor_type VARCHAR(16) NULL,
  withdrawn_by_actor_id INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- Part 1: only Active Instructor rows occupy the unique slot (see sql/course_part1_active_instructor_uk.sql).
  instructor_course_id INT GENERATED ALWAYS AS (IF(course_role = 'Instructor' AND active = 1, course_id, NULL)) STORED,
  PRIMARY KEY (id),
  UNIQUE KEY uk_enrollment_course_user (course_id, user_id),
  UNIQUE KEY uk_enrollment_one_instructor (instructor_course_id),
  KEY idx_enrollment_user (user_id),
  KEY idx_enrollment_course (course_id),
  KEY idx_enrollment_course_active (course_id, active),
  CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE RESTRICT,
  CONSTRAINT fk_enrollment_user FOREIGN KEY (user_id) REFERENCES user (id)
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
