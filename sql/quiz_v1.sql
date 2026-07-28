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
