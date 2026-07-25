-- CourseEvent: one-off dated events (not recurring sessions)

CREATE TABLE IF NOT EXISTS course_event (
  id INT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  name VARCHAR(255) NOT NULL,
  event_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  location VARCHAR(255) NULL,
  description TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_course_event_course (course_id),
  KEY idx_course_event_date (event_date),
  CONSTRAINT fk_course_event_course FOREIGN KEY (course_id) REFERENCES Course (id) ON DELETE CASCADE,
  CONSTRAINT chk_course_event_time CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
