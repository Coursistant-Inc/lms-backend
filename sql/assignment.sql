-- Assignment module (Individual Tier-1) for lms_v2
-- Authoritative schema per Part 9 / Phase 0a. No submission_status / late boolean columns.

CREATE TABLE IF NOT EXISTS assignment (
  id INT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  description MEDIUMTEXT NOT NULL,
  points_possible DECIMAL(10, 2) NOT NULL,
  due_at DATETIME NOT NULL,
  late_until DATETIME NULL,
  submission_type VARCHAR(32) NOT NULL DEFAULT 'Individual',
  group_set_id INT NULL,
  allowed_file_types JSON NOT NULL,
  max_file_size_bytes BIGINT NOT NULL,
  max_file_count INT NOT NULL,
  state ENUM('Draft', 'Published') NOT NULL DEFAULT 'Draft',
  current_rubric_version_id INT NULL,
  created_by INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_assignment_course (course_id),
  KEY idx_assignment_course_due (course_id, due_at, id),
  KEY idx_assignment_state (course_id, state),
  CONSTRAINT fk_assignment_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE RESTRICT,
  CONSTRAINT fk_assignment_created_by FOREIGN KEY (created_by) REFERENCES user (id) ON DELETE RESTRICT,
  CONSTRAINT chk_assignment_points CHECK (points_possible > 0),
  CONSTRAINT chk_assignment_max_count CHECK (max_file_count >= 1 AND max_file_count <= 10),
  CONSTRAINT chk_assignment_max_size CHECK (max_file_size_bytes >= 1 AND max_file_size_bytes <= 104857600),
  CONSTRAINT chk_assignment_late_until CHECK (late_until IS NULL OR late_until >= due_at),
  CONSTRAINT fk_assignment_group_set FOREIGN KEY (group_set_id) REFERENCES group_set (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assignment_attachment (
  id INT NOT NULL AUTO_INCREMENT,
  assignment_id INT NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(128) NULL,
  size_bytes BIGINT NOT NULL,
  uploaded_by INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_assignment_attachment_assignment (assignment_id),
  CONSTRAINT fk_assignment_attachment_assignment FOREIGN KEY (assignment_id) REFERENCES assignment (id) ON DELETE CASCADE,
  CONSTRAINT fk_assignment_attachment_uploader FOREIGN KEY (uploaded_by) REFERENCES user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assignment_rubric_version (
  id INT NOT NULL AUTO_INCREMENT,
  assignment_id INT NOT NULL,
  version_no INT NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(128) NOT NULL,
  size_bytes BIGINT NOT NULL,
  uploaded_by INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_assignment_rubric_version (assignment_id, version_no),
  KEY idx_assignment_rubric_assignment (assignment_id),
  CONSTRAINT fk_assignment_rubric_assignment FOREIGN KEY (assignment_id) REFERENCES assignment (id) ON DELETE CASCADE,
  CONSTRAINT fk_assignment_rubric_uploader FOREIGN KEY (uploaded_by) REFERENCES user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Optional FK from assignment.current_rubric_version_id (added after rubric table exists)
ALTER TABLE assignment
  ADD CONSTRAINT fk_assignment_current_rubric
  FOREIGN KEY (current_rubric_version_id) REFERENCES assignment_rubric_version (id)
  ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS assignment_audit_log (
  id INT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  assignment_id INT NOT NULL,
  actor_user_id INT NOT NULL,
  action VARCHAR(64) NOT NULL,
  detail_json TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_assignment_audit_assignment (assignment_id),
  KEY idx_assignment_audit_course (course_id),
  CONSTRAINT fk_assignment_audit_assignment FOREIGN KEY (assignment_id) REFERENCES assignment (id) ON DELETE CASCADE,
  CONSTRAINT fk_assignment_audit_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE RESTRICT,
  CONSTRAINT fk_assignment_audit_actor FOREIGN KEY (actor_user_id) REFERENCES user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assignment_submission (
  id INT NOT NULL AUTO_INCREMENT,
  assignment_id INT NOT NULL,
  owner_user_id INT NULL,
  group_id INT NULL,
  current_version_id INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_assignment_submission_owner (assignment_id, owner_user_id),
  UNIQUE KEY uk_assignment_submission_group (assignment_id, group_id),
  KEY idx_assignment_submission_assignment (assignment_id),
  KEY idx_assignment_submission_owner (owner_user_id),
  KEY idx_assignment_submission_group (group_id),
  CONSTRAINT fk_assignment_submission_assignment FOREIGN KEY (assignment_id) REFERENCES assignment (id) ON DELETE CASCADE,
  CONSTRAINT fk_assignment_submission_owner FOREIGN KEY (owner_user_id) REFERENCES user (id) ON DELETE RESTRICT,
  CONSTRAINT fk_assignment_submission_group FOREIGN KEY (group_id) REFERENCES course_group (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assignment_submission_version (
  id INT NOT NULL AUTO_INCREMENT,
  submission_id INT NOT NULL,
  assignment_id INT NOT NULL,
  owner_user_id INT NOT NULL,
  actual_submitter_user_id INT NOT NULL,
  version_no INT NOT NULL,
  submitted_at DATETIME NOT NULL,
  used_grace_buffer TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_assignment_submission_version_no (submission_id, version_no),
  KEY idx_assignment_submission_version_submission (submission_id),
  KEY idx_assignment_submission_version_assignment (assignment_id),
  CONSTRAINT fk_assignment_submission_version_submission FOREIGN KEY (submission_id) REFERENCES assignment_submission (id) ON DELETE CASCADE,
  CONSTRAINT fk_assignment_submission_version_assignment FOREIGN KEY (assignment_id) REFERENCES assignment (id) ON DELETE CASCADE,
  CONSTRAINT fk_assignment_submission_version_owner FOREIGN KEY (owner_user_id) REFERENCES user (id) ON DELETE RESTRICT,
  CONSTRAINT fk_assignment_submission_version_submitter FOREIGN KEY (actual_submitter_user_id) REFERENCES user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE assignment_submission
  ADD CONSTRAINT fk_assignment_submission_current_version
  FOREIGN KEY (current_version_id) REFERENCES assignment_submission_version (id)
  ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS assignment_submission_receipt (
  id INT NOT NULL AUTO_INCREMENT,
  submission_version_id INT NOT NULL,
  issued_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_assignment_submission_receipt_version (submission_version_id),
  CONSTRAINT fk_assignment_submission_receipt_version FOREIGN KEY (submission_version_id) REFERENCES assignment_submission_version (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assignment_submission_file (
  id INT NOT NULL AUTO_INCREMENT,
  submission_version_id INT NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(128) NULL,
  size_bytes BIGINT NOT NULL,
  checksum_sha256 CHAR(64) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_assignment_submission_file_version (submission_version_id),
  CONSTRAINT fk_assignment_submission_file_version FOREIGN KEY (submission_version_id) REFERENCES assignment_submission_version (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assignment_submission_staging_file (
  id INT NOT NULL AUTO_INCREMENT,
  assignment_id INT NOT NULL,
  owner_user_id INT NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(128) NULL,
  size_bytes BIGINT NOT NULL,
  checksum_sha256 CHAR(64) NOT NULL,
  consumed TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  expires_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_assignment_staging_owner (assignment_id, owner_user_id, consumed),
  CONSTRAINT fk_assignment_staging_assignment FOREIGN KEY (assignment_id) REFERENCES assignment (id) ON DELETE CASCADE,
  CONSTRAINT fk_assignment_staging_owner FOREIGN KEY (owner_user_id) REFERENCES user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assignment_grade (
  id INT NOT NULL AUTO_INCREMENT,
  assignment_id INT NOT NULL,
  student_user_id INT NULL,
  group_id INT NULL,
  submission_version_id INT NULL,
  rubric_version_id INT NULL,
  score DECIMAL(10, 2) NOT NULL,
  feedback_html MEDIUMTEXT NULL,
  annotated_object_key VARCHAR(512) NULL,
  annotated_original_name VARCHAR(255) NULL,
  annotated_content_type VARCHAR(128) NULL,
  annotated_size_bytes BIGINT NULL,
  status ENUM('Entered', 'Released') NOT NULL DEFAULT 'Entered',
  entered_by INT NOT NULL,
  entered_at DATETIME NOT NULL,
  edited_by INT NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  released_at DATETIME NULL,
  ai_assisted TINYINT(1) NOT NULL DEFAULT 0,
  ai_provenance_json JSON NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_assignment_grade_student (assignment_id, student_user_id),
  UNIQUE KEY uk_assignment_grade_group (assignment_id, group_id),
  KEY idx_assignment_grade_assignment (assignment_id),
  KEY idx_assignment_grade_student (student_user_id),
  KEY idx_assignment_grade_group (group_id),
  CONSTRAINT fk_assignment_grade_assignment FOREIGN KEY (assignment_id) REFERENCES assignment (id) ON DELETE CASCADE,
  CONSTRAINT fk_assignment_grade_student FOREIGN KEY (student_user_id) REFERENCES user (id) ON DELETE RESTRICT,
  CONSTRAINT fk_assignment_grade_group FOREIGN KEY (group_id) REFERENCES course_group (id) ON DELETE RESTRICT,
  CONSTRAINT fk_assignment_grade_version FOREIGN KEY (submission_version_id) REFERENCES assignment_submission_version (id) ON DELETE SET NULL,
  CONSTRAINT fk_assignment_grade_rubric FOREIGN KEY (rubric_version_id) REFERENCES assignment_rubric_version (id) ON DELETE SET NULL,
  CONSTRAINT fk_assignment_grade_entered_by FOREIGN KEY (entered_by) REFERENCES user (id) ON DELETE RESTRICT,
  CONSTRAINT fk_assignment_grade_edited_by FOREIGN KEY (edited_by) REFERENCES user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assignment_grade_release_recipient (
  id INT NOT NULL AUTO_INCREMENT,
  grade_id INT NOT NULL,
  assignment_id INT NOT NULL,
  group_id INT NOT NULL,
  student_user_id INT NOT NULL,
  released_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_grade_release_recipient (grade_id, student_user_id),
  KEY idx_grade_release_recipient_student (assignment_id, student_user_id),
  CONSTRAINT fk_grade_release_recipient_grade FOREIGN KEY (grade_id) REFERENCES assignment_grade (id) ON DELETE CASCADE,
  CONSTRAINT fk_grade_release_recipient_assignment FOREIGN KEY (assignment_id) REFERENCES assignment (id) ON DELETE CASCADE,
  CONSTRAINT fk_grade_release_recipient_group FOREIGN KEY (group_id) REFERENCES course_group (id) ON DELETE RESTRICT,
  CONSTRAINT fk_grade_release_recipient_student FOREIGN KEY (student_user_id) REFERENCES user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
