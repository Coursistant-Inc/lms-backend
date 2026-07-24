-- Course table for lms_v2 (MySQL)
-- instructor_id / creator_id reference User(id); tenant_id has no Tenant table yet.
-- course_code is intentionally not unique (duplicates allowed).

CREATE TABLE Course (
  id INT NOT NULL AUTO_INCREMENT,
  tenant_id INT NOT NULL,
  course_code VARCHAR(32) NOT NULL,
  title VARCHAR(255) NOT NULL,
  term_start_date DATE NOT NULL,
  term_end_date DATE NOT NULL,
  description TEXT NULL,
  location VARCHAR(255) NULL,
  instructor_id INT NOT NULL,
  state ENUM('Active', 'Archived') NOT NULL DEFAULT 'Active',
  archived_at DATETIME NULL,
  creator_id INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_course_tenant (tenant_id),
  KEY idx_course_state (state),
  KEY idx_course_instructor (instructor_id),
  KEY idx_course_creator (creator_id),
  CONSTRAINT fk_course_instructor FOREIGN KEY (instructor_id) REFERENCES User (id),
  CONSTRAINT fk_course_creator FOREIGN KEY (creator_id) REFERENCES User (id),
  CONSTRAINT chk_course_term CHECK (term_end_date >= term_start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- If the table was created with the old unique index, run:
-- ALTER TABLE Course DROP INDEX uk_tenant_course_code;
