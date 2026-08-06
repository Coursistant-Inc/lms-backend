-- course_part1_restore.sql
-- Rollback drill for Part 1 expand + UK tighten. Does NOT restore dropped data.
-- 1) Revert instructor_course_id to pre-Part1 expression (active not required).
-- 2) Drop creator actor columns and course_audit_log.

ALTER TABLE enrollment DROP INDEX uk_enrollment_one_instructor;

ALTER TABLE enrollment
  DROP COLUMN instructor_course_id,
  ADD COLUMN instructor_course_id INT
    GENERATED ALWAYS AS (IF(course_role = 'Instructor', course_id, NULL)) STORED
    AFTER updated_at;

ALTER TABLE enrollment
  ADD UNIQUE KEY uk_enrollment_one_instructor (instructor_course_id);

ALTER TABLE course DROP COLUMN creator_role;
ALTER TABLE course DROP COLUMN creator_actor_id;
ALTER TABLE course DROP COLUMN creator_actor_type;

DROP TABLE IF EXISTS course_audit_log;
