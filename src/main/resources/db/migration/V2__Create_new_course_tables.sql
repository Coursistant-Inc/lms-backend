CREATE TABLE IF NOT EXISTS nw_course (
                                         id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                         created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    course_code VARCHAR(16) NOT NULL,
    name VARCHAR(127) NOT NULL,
    description TEXT NOT NULL,
    school VARCHAR(127) NOT NULL,
    semester VARCHAR(127) NOT NULL,
    teacher_id INT NOT NULL,

    CONSTRAINT uk_nw_course_code UNIQUE (course_code),
    CONSTRAINT fk_nw_course_teacher
    FOREIGN KEY (teacher_id)
    REFERENCES `User` (id)
    ON DELETE RESTRICT
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS nw_course_unit (
                                              id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                              created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    sort_order INT NOT NULL,
    title VARCHAR(63) NOT NULL,
    description TEXT NOT NULL,
    course_id BIGINT NOT NULL,

    CONSTRAINT fk_nw_course_unit_course
    FOREIGN KEY (course_id)
    REFERENCES nw_course (id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS nw_assignment (
                                             id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                             created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    title VARCHAR(63) NOT NULL,
    description TEXT NOT NULL,
    type VARCHAR(31) NOT NULL,
    due_time TIMESTAMP(6) NOT NULL,
    settings JSON NOT NULL,
    course_unit_id BIGINT NOT NULL,

    CONSTRAINT fk_nw_assignment_course_unit
    FOREIGN KEY (course_unit_id)
    REFERENCES nw_course_unit (id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_nw_course_teacher ON nw_course(teacher_id);
CREATE INDEX idx_nw_course_unit_course ON nw_course_unit(course_id);
CREATE INDEX idx_nw_assignment_course_unit ON nw_assignment(course_unit_id);