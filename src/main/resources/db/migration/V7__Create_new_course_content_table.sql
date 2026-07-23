CREATE TABLE IF NOT EXISTS nw_course_content(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    CONSTRAINT fk_course_content_course
    FOREIGN KEY(course_id)
    REFERENCES nw_course(id),
    CONSTRAINT fk_course_content_file_id
    FOREIGN KEY(file_id)
    REFERENCES nw_file_reference(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


