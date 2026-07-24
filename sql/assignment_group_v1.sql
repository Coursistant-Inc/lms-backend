-- Group Assignment V1 (MySQL 5.7 compatible — CHECK enforced in app layer)

-- 1) FK from assignment.group_set_id -> group_set
ALTER TABLE assignment
  ADD CONSTRAINT fk_assignment_group_set
  FOREIGN KEY (group_set_id) REFERENCES group_set (id) ON DELETE RESTRICT;

-- 2) Submission owner: user XOR group (app-enforced)
ALTER TABLE assignment_submission
  DROP FOREIGN KEY fk_assignment_submission_owner;

ALTER TABLE assignment_submission
  DROP INDEX uk_assignment_submission_owner;

ALTER TABLE assignment_submission
  MODIFY COLUMN owner_user_id INT NULL,
  ADD COLUMN group_id INT NULL AFTER owner_user_id;

ALTER TABLE assignment_submission
  ADD CONSTRAINT fk_assignment_submission_owner FOREIGN KEY (owner_user_id) REFERENCES user (id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_assignment_submission_group FOREIGN KEY (group_id) REFERENCES course_group (id) ON DELETE RESTRICT,
  ADD UNIQUE KEY uk_assignment_submission_owner (assignment_id, owner_user_id),
  ADD UNIQUE KEY uk_assignment_submission_group (assignment_id, group_id);

-- 3) Version actual submitter
ALTER TABLE assignment_submission_version
  ADD COLUMN actual_submitter_user_id INT NULL AFTER owner_user_id;

UPDATE assignment_submission_version
SET actual_submitter_user_id = owner_user_id
WHERE actual_submitter_user_id IS NULL;

ALTER TABLE assignment_submission_version
  MODIFY COLUMN actual_submitter_user_id INT NOT NULL;

ALTER TABLE assignment_submission_version
  ADD CONSTRAINT fk_assignment_submission_version_submitter
    FOREIGN KEY (actual_submitter_user_id) REFERENCES user (id) ON DELETE RESTRICT;

-- 4) Grade dual form
ALTER TABLE assignment_grade
  DROP FOREIGN KEY fk_assignment_grade_student;

ALTER TABLE assignment_grade
  DROP INDEX uk_assignment_grade_student;

ALTER TABLE assignment_grade
  MODIFY COLUMN student_user_id INT NULL,
  ADD COLUMN group_id INT NULL AFTER student_user_id;

ALTER TABLE assignment_grade
  ADD CONSTRAINT fk_assignment_grade_student FOREIGN KEY (student_user_id) REFERENCES user (id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_assignment_grade_group FOREIGN KEY (group_id) REFERENCES course_group (id) ON DELETE RESTRICT,
  ADD UNIQUE KEY uk_assignment_grade_student (assignment_id, student_user_id),
  ADD UNIQUE KEY uk_assignment_grade_group (assignment_id, group_id);

-- 5) Release membership snapshot
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
