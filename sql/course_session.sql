-- CourseSession: weekly schedule template (one row per day)
-- Apply against lms_v2

CREATE TABLE IF NOT EXISTS course_session (
  id INT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  type ENUM('Lecture', 'Lab', 'Tutorial') NOT NULL,
  day_of_week ENUM('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN') NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  location VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_course_session_course (course_id),
  CONSTRAINT fk_course_session_course FOREIGN KEY (course_id) REFERENCES Course (id) ON DELETE CASCADE,
  CONSTRAINT chk_course_session_time CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
