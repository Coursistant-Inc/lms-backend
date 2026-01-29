CREATE TABLE IF NOT EXISTS nw_file_reference (
                                                 id BIGINT NOT NULL AUTO_INCREMENT,
                                                 created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    file_name VARCHAR(127) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(63) NOT NULL,
    file_path TEXT(500) NOT NULL,
    entity_type VARCHAR(63) NOT NULL,
    entity_id BIGINT NOT NULL,
    upload_user_id INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_nw_file_reference_upload_user
    FOREIGN KEY (upload_user_id) REFERENCES User (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_file_reference_entity ON nw_file_reference (entity_type, entity_id);
CREATE INDEX idx_file_reference_upload_user ON nw_file_reference (upload_user_id);

CREATE TABLE IF NOT EXISTS nw_submission (
                                             id BIGINT NOT NULL AUTO_INCREMENT,
                                             created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    submission_count INT NOT NULL,
    submission_content TEXT(1000) NOT NULL,
    student_id BIGINT NOT NULL,
    assignment_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_nw_submission_student
    FOREIGN KEY (student_id) REFERENCES User (id),
    CONSTRAINT fk_nw_submission_assignment
    FOREIGN KEY (assignment_id) REFERENCES nw_assignment (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_submission_student ON nw_submission (student_id);
CREATE INDEX idx_submission_assignment ON nw_submission (assignment_id);
CREATE UNIQUE INDEX idx_submission_unique_assignment_student
    ON nw_submission (assignment_id, student_id);

CREATE TABLE IF NOT EXISTS nw_review (
                                         id BIGINT NOT NULL AUTO_INCREMENT,
                                         created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    grade INT NOT NULL,
    teacher_comment TEXT(1000) NOT NULL,
    submission_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_nw_review_submission
    FOREIGN KEY (submission_id) REFERENCES nw_submission (id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_review_submission ON nw_review (submission_id);
CREATE INDEX idx_review_grade ON nw_review (grade);
CREATE UNIQUE INDEX idx_review_unique_submission ON nw_review (submission_id);