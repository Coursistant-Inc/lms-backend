-- course_part1_active_instructor_uk.sql
-- Tighten generated column so only Active Instructors occupy the unique slot.
-- Order: drop UK → rebuild generated column → add UK.

ALTER TABLE enrollment DROP INDEX uk_enrollment_one_instructor;

ALTER TABLE enrollment
  DROP COLUMN instructor_course_id,
  ADD COLUMN instructor_course_id INT
    GENERATED ALWAYS AS (IF(course_role = 'Instructor' AND active = 1, course_id, NULL)) STORED
    AFTER updated_at;

ALTER TABLE enrollment
  ADD UNIQUE KEY uk_enrollment_one_instructor (instructor_course_id);
