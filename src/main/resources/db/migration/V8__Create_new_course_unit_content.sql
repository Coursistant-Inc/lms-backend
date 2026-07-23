CREATE TABLE IF NOT EXISTS nw_course_unit_content(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_unit_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    CONSTRAINT fk_course_unit_content_course_unit
    FOREIGN KEY(course_unit_id)
    REFERENCES nw_course_unit(id),
    CONSTRAINT fk_nw_course_unit_file_reference
    FOREIGN KEY(file_id)
    REFERENCES nw_file_reference(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;