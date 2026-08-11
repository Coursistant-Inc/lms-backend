-- TA Identity IT additive schema (auth-it-schema applied first)
SET NAMES utf8mb4;
-- Course table for lms_v2 (MySQL)
-- instructor_id / creator_id reference User(id); tenant_id has no Tenant table yet.
-- course_code is intentionally not unique (duplicates allowed).

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
  state ENUM('Active', 'Archived') NOT NULL DEFAULT 'Active',
  archived_at DATETIME NULL,
  archived_by_actor_type VARCHAR(16) NULL,
  archived_by_actor_id INT NULL,
  creator_id INT NOT NULL,
  -- Part 1 actor fields (see sql/course_part1_expand.sql); creator_id retained for one release.
  creator_actor_type VARCHAR(16) NULL,
  creator_actor_id INT NULL,
  creator_role VARCHAR(32) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_course_tenant (tenant_id),
  KEY idx_course_state (state),
  KEY idx_course_instructor (instructor_id),
  KEY idx_course_creator (creator_id),
  CONSTRAINT fk_course_instructor FOREIGN KEY (instructor_id) REFERENCES user (id),
  CONSTRAINT fk_course_creator FOREIGN KEY (creator_id) REFERENCES user (id),
  CONSTRAINT chk_course_term CHECK (term_end_date >= term_start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- course_audit_log: see sql/course_part1_expand.sql

-- If the table was created with the old unique index, run:
-- ALTER TABLE Course DROP INDEX uk_tenant_course_code;

CREATE TABLE IF NOT EXISTS course_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  tenant_id INT NULL,
  actor_type VARCHAR(16) NOT NULL,
  actor_id INT NOT NULL,
  actor_role VARCHAR(32) NULL,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(32) NULL,
  target_id INT NULL,
  before_json JSON NULL,
  after_json JSON NULL,
  request_id VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_course_audit_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- ==== enrollment.sql ====
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

-- ==== group.sql ====
-- Group module for lms_v2 (GroupSet 鈫?CourseGroup 鈫?GroupMembership + audit)

CREATE TABLE IF NOT EXISTS group_set (
  id INT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  name VARCHAR(255) NOT NULL,
  default_capacity INT NOT NULL,
  join_opens_at DATETIME NULL,
  join_closes_at DATETIME NULL,
  locked TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_group_set_course (course_id),
  CONSTRAINT fk_group_set_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE RESTRICT,
  CONSTRAINT chk_group_set_default_capacity CHECK (default_capacity >= 1 AND default_capacity <= 200)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course_group (
  id INT NOT NULL AUTO_INCREMENT,
  group_set_id INT NOT NULL,
  course_id INT NOT NULL,
  name VARCHAR(255) NOT NULL,
  capacity_override INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_course_group_set (group_set_id),
  KEY idx_course_group_course (course_id),
  CONSTRAINT fk_course_group_set FOREIGN KEY (group_set_id) REFERENCES group_set (id) ON DELETE CASCADE,
  CONSTRAINT fk_course_group_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE RESTRICT,
  CONSTRAINT chk_course_group_capacity_override CHECK (
    capacity_override IS NULL OR (capacity_override >= 1 AND capacity_override <= 200)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS group_membership (
  id INT NOT NULL AUTO_INCREMENT,
  group_id INT NOT NULL,
  group_set_id INT NOT NULL,
  course_id INT NOT NULL,
  user_id INT NOT NULL,
  joined_at DATETIME NOT NULL,
  added_by_type ENUM('Self', 'Staff') NOT NULL,
  added_by_user_id INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_membership_set_user (group_set_id, user_id),
  KEY idx_membership_group (group_id),
  KEY idx_membership_course_user (course_id, user_id),
  CONSTRAINT fk_membership_group FOREIGN KEY (group_id) REFERENCES course_group (id) ON DELETE CASCADE,
  CONSTRAINT fk_membership_set FOREIGN KEY (group_set_id) REFERENCES group_set (id) ON DELETE CASCADE,
  CONSTRAINT fk_membership_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE RESTRICT,
  CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES user (id),
  CONSTRAINT fk_membership_added_by FOREIGN KEY (added_by_user_id) REFERENCES user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS group_membership_audit (
  id INT NOT NULL AUTO_INCREMENT,
  tenant_id INT NOT NULL,
  course_id INT NOT NULL,
  group_set_id INT NULL,
  group_id INT NULL,
  target_user_id INT NOT NULL,
  actor_type ENUM('USER', 'ADMIN', 'SYSTEM') NOT NULL,
  actor_user_id INT NULL,
  action VARCHAR(64) NOT NULL,
  before_json TEXT NULL,
  after_json TEXT NULL,
  detail_json TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_group_membership_audit_course (course_id),
  KEY idx_group_membership_audit_set (group_set_id),
  KEY idx_group_membership_audit_target (target_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==== assignment.sql ====
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

-- ==== quiz_v1.sql ====
-- Quiz Module V1 schema for lms_v2
-- INT AUTO_INCREMENT, UTC DATETIME(3), snake_case

CREATE TABLE IF NOT EXISTS quiz (
  id INT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  title VARCHAR(200) NOT NULL,
  instructions TEXT NULL,
  opens_at DATETIME(3) NOT NULL,
  closes_at DATETIME(3) NOT NULL,
  time_limit_seconds INT NULL,
  attempts_allowed INT NOT NULL DEFAULT 1,
  result_visibility VARCHAR(32) NOT NULL,
  state VARCHAR(16) NOT NULL,
  version INT NOT NULL DEFAULT 1,
  created_by INT NOT NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_quiz_course_state (course_id, state),
  KEY idx_quiz_course_window (course_id, opens_at, closes_at),
  CONSTRAINT fk_quiz_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE RESTRICT,
  CONSTRAINT fk_quiz_created_by FOREIGN KEY (created_by) REFERENCES user (id) ON DELETE RESTRICT,
  CONSTRAINT chk_quiz_attempts CHECK (attempts_allowed >= 1),
  CONSTRAINT chk_quiz_window CHECK (opens_at < closes_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quiz_question (
  id INT NOT NULL AUTO_INCREMENT,
  quiz_id INT NOT NULL,
  type VARCHAR(32) NOT NULL,
  stem TEXT NOT NULL,
  points DECIMAL(10,2) NOT NULL,
  position INT NOT NULL,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_quiz_question_position (quiz_id, position),
  KEY idx_quiz_question_quiz (quiz_id),
  CONSTRAINT fk_quiz_question_quiz FOREIGN KEY (quiz_id) REFERENCES quiz (id) ON DELETE CASCADE,
  CONSTRAINT chk_quiz_question_points CHECK (points >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quiz_question_option (
  id INT NOT NULL AUTO_INCREMENT,
  question_id INT NOT NULL,
  label VARCHAR(500) NOT NULL,
  is_correct TINYINT(1) NOT NULL DEFAULT 0,
  position INT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_quiz_option_position (question_id, position),
  KEY idx_quiz_option_question (question_id),
  CONSTRAINT fk_quiz_option_question FOREIGN KEY (question_id) REFERENCES quiz_question (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quiz_attempt (
  id INT NOT NULL AUTO_INCREMENT,
  quiz_id INT NOT NULL,
  user_id INT NOT NULL,
  attempt_number INT NOT NULL,
  status VARCHAR(16) NOT NULL,
  close_reason VARCHAR(32) NULL,
  receipt_id CHAR(36) NULL,
  started_at DATETIME(3) NOT NULL,
  deadline_at DATETIME(3) NOT NULL,
  submitted_at DATETIME(3) NULL,
  auto_score DECIMAL(10,2) NULL,
  manual_score DECIMAL(10,2) NULL,
  total_score DECIMAL(10,2) NULL,
  manual_grading_complete TINYINT(1) NOT NULL DEFAULT 0,
  in_progress_owner INT
    GENERATED ALWAYS AS (
      CASE
        WHEN status IN ('InProgress', 'Finalizing') THEN user_id
        ELSE NULL
      END
    ) STORED,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_quiz_attempt_in_progress (quiz_id, in_progress_owner),
  UNIQUE KEY uk_quiz_attempt_number (quiz_id, user_id, attempt_number),
  UNIQUE KEY uk_quiz_attempt_receipt (receipt_id),
  KEY idx_quiz_attempt_deadline (quiz_id, status, deadline_at),
  KEY idx_quiz_attempt_user (user_id, quiz_id),
  CONSTRAINT fk_quiz_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES quiz (id) ON DELETE RESTRICT,
  CONSTRAINT fk_quiz_attempt_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quiz_attempt_answer (
  id INT NOT NULL AUTO_INCREMENT,
  attempt_id INT NOT NULL,
  question_id INT NOT NULL,
  selected_option_ids_json JSON NULL,
  text_answer TEXT NULL,
  revision INT NOT NULL DEFAULT 1,
  saved_at DATETIME(3) NOT NULL,
  score DECIMAL(10,2) NULL,
  pending_manual TINYINT(1) NOT NULL DEFAULT 0,
  feedback TEXT NULL,
  graded_by INT NULL,
  graded_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_quiz_answer_attempt_question (attempt_id, question_id),
  KEY idx_quiz_answer_question (question_id),
  CONSTRAINT fk_quiz_answer_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempt (id) ON DELETE CASCADE,
  CONSTRAINT fk_quiz_answer_question FOREIGN KEY (question_id) REFERENCES quiz_question (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quiz_grade (
  id INT NOT NULL AUTO_INCREMENT,
  quiz_id INT NOT NULL,
  user_id INT NOT NULL,
  counted_attempt_id INT NOT NULL,
  status VARCHAR(16) NOT NULL,
  version INT NOT NULL DEFAULT 1,
  released_at DATETIME(3) NULL,
  released_by INT NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_quiz_grade_user (quiz_id, user_id),
  KEY idx_quiz_grade_user_status (user_id, status),
  CONSTRAINT fk_quiz_grade_quiz FOREIGN KEY (quiz_id) REFERENCES quiz (id) ON DELETE RESTRICT,
  CONSTRAINT fk_quiz_grade_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE RESTRICT,
  CONSTRAINT fk_quiz_grade_attempt FOREIGN KEY (counted_attempt_id) REFERENCES quiz_attempt (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quiz_score_audit (
  id INT NOT NULL AUTO_INCREMENT,
  quiz_id INT NOT NULL,
  attempt_id INT NOT NULL,
  question_id INT NOT NULL,
  actor_user_id INT NOT NULL,
  reason VARCHAR(500) NULL,
  score_before DECIMAL(10,2) NULL,
  score_after DECIMAL(10,2) NULL,
  created_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_quiz_score_audit_attempt (attempt_id),
  KEY idx_quiz_score_audit_quiz (quiz_id),
  CONSTRAINT fk_quiz_score_audit_quiz FOREIGN KEY (quiz_id) REFERENCES quiz (id) ON DELETE RESTRICT,
  CONSTRAINT fk_quiz_score_audit_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempt (id) ON DELETE RESTRICT,
  CONSTRAINT fk_quiz_score_audit_question FOREIGN KEY (question_id) REFERENCES quiz_question (id) ON DELETE RESTRICT,
  CONSTRAINT fk_quiz_score_audit_actor FOREIGN KEY (actor_user_id) REFERENCES user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quiz_audit_log (
  id INT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  quiz_id INT NULL,
  attempt_id INT NULL,
  actor_user_id INT NULL,
  action VARCHAR(64) NOT NULL,
  reason VARCHAR(500) NULL,
  detail_json JSON NOT NULL,
  created_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_quiz_audit_course (course_id, created_at),
  KEY idx_quiz_audit_quiz (quiz_id, created_at),
  CONSTRAINT fk_quiz_audit_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

