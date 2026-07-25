-- Rename concatenated lowercase tables to MySQL snake_case.
-- Run against lms_v2 during a maintenance window (stop app first).
-- Idempotent: only renames when source exists and target does not.

USE lms_v2;

-- Precheck (inspect before/after):
-- SHOW TABLES;

DELIMITER $$

DROP PROCEDURE IF EXISTS rename_table_if_needed $$
CREATE PROCEDURE rename_table_if_needed(
    IN src_name VARCHAR(64),
    IN dst_name VARCHAR(64)
)
BEGIN
    DECLARE src_count INT DEFAULT 0;
    DECLARE dst_count INT DEFAULT 0;

    SELECT COUNT(*) INTO src_count
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = src_name;

    SELECT COUNT(*) INTO dst_count
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = dst_name;

    IF src_count > 0 AND dst_count = 0 THEN
        SET @sql = CONCAT('RENAME TABLE `', src_name, '` TO `', dst_name, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('RENAMED ', src_name, ' -> ', dst_name) AS result;
    ELSEIF dst_count > 0 THEN
        SELECT CONCAT('SKIP (target exists): ', dst_name) AS result;
    ELSE
        SELECT CONCAT('SKIP (source missing): ', src_name) AS result;
    END IF;
END $$

DELIMITER ;

CALL rename_table_if_needed('courseevent', 'course_event');
CALL rename_table_if_needed('coursematerial', 'course_material');
CALL rename_table_if_needed('coursesession', 'course_session');
CALL rename_table_if_needed('coursesyllabus', 'course_syllabus');
CALL rename_table_if_needed('coursesyllabusversion', 'course_syllabus_version');
CALL rename_table_if_needed('courseweek', 'course_week');
CALL rename_table_if_needed('enrollmentauditlog', 'enrollment_audit_log');

-- Also handle PascalCase leftovers if any environment still has them
CALL rename_table_if_needed('CourseEvent', 'course_event');
CALL rename_table_if_needed('CourseMaterial', 'course_material');
CALL rename_table_if_needed('CourseSession', 'course_session');
CALL rename_table_if_needed('CourseSyllabus', 'course_syllabus');
CALL rename_table_if_needed('CourseSyllabusVersion', 'course_syllabus_version');
CALL rename_table_if_needed('CourseWeek', 'course_week');
CALL rename_table_if_needed('EnrollmentAuditLog', 'enrollment_audit_log');
CALL rename_table_if_needed('User', 'user');
CALL rename_table_if_needed('Admin', 'admin');
CALL rename_table_if_needed('Course', 'course');
CALL rename_table_if_needed('Enrollment', 'enrollment');

DROP PROCEDURE IF EXISTS rename_table_if_needed;

SHOW TABLES;

-- ---------------------------------------------------------------------------
-- ROLLBACK (manual, only if you must revert to concatenated names)
-- ---------------------------------------------------------------------------
-- RENAME TABLE
--   course_event TO courseevent,
--   course_material TO coursematerial,
--   course_session TO coursesession,
--   course_syllabus TO coursesyllabus,
--   course_syllabus_version TO coursesyllabusversion,
--   course_week TO courseweek,
--   enrollment_audit_log TO enrollmentauditlog;
