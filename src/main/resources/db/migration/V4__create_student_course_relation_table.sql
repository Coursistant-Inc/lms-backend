CREATE TABLE IF NOT EXISTS rel_user_course (
                                                    id BIGINT NOT NULL AUTO_INCREMENT,
                                                    user_id INT NOT NULL,
                                                    course_id BIGINT NOT NULL,
                                                    PRIMARY KEY (id),
    CONSTRAINT fk_rel_course_user
    FOREIGN KEY (user_id) REFERENCES User (id),
    CONSTRAINT fk_rel_user_course
    FOREIGN KEY (course_id) REFERENCES nw_course (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX idx_unique_user_course ON rel_user_course (user_id, course_id);
CREATE INDEX idx_user_id ON rel_user_course (user_id);
CREATE INDEX idx_course_id ON rel_user_course (course_id);