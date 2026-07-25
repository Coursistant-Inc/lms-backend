-- CourseSyllabus + CourseSyllabusVersion for lms_v2

CREATE TABLE IF NOT EXISTS course_syllabus_version (
  id INT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  content_type VARCHAR(128) NOT NULL,
  size_bytes BIGINT NOT NULL,
  uploaded_by INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_course_syllabus_version_course (course_id),
  CONSTRAINT fk_course_syllabus_version_course FOREIGN KEY (course_id) REFERENCES Course (id) ON DELETE CASCADE,
  CONSTRAINT fk_course_syllabus_version_user FOREIGN KEY (uploaded_by) REFERENCES User (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- CourseSyllabus: one row per course tracking the current/previous posted version.
-- current_version_id / previous_version_id intentionally have NO foreign key
-- constraint pointing at CourseSyllabusVersion, to avoid a circular FK
-- dependency between the two tables. Referential integrity for these two
-- columns is enforced in the application/service layer.
CREATE TABLE IF NOT EXISTS course_syllabus (
  course_id INT NOT NULL,
  current_version_id INT NULL,
  previous_version_id INT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (course_id),
  CONSTRAINT fk_course_syllabus_course FOREIGN KEY (course_id) REFERENCES Course (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
