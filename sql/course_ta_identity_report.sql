-- course_ta_identity_report.sql
-- Read-only report for Local/Dev deploy gate. Does NOT modify data.
-- Blocking Active-level mismatches are also in course_part1_precheck.sql.
-- Deploy only when Active violation counts are 0.

-- Active TA with non-STUDENT level (must be 0 before deploy)
SELECT 'active_ta_level_not_student' AS report, e.id AS enrollment_id, e.course_id, e.user_id,
       u.role, u.level, e.active
FROM enrollment e
INNER JOIN `user` u ON u.id = e.user_id
WHERE e.active = 1
  AND e.course_role = 'TA'
  AND (u.level IS NULL OR UPPER(u.level) <> 'STUDENT');

SELECT 'active_ta_level_not_student_cnt' AS report, COUNT(*) AS cnt
FROM enrollment e
INNER JOIN `user` u ON u.id = e.user_id
WHERE e.active = 1
  AND e.course_role = 'TA'
  AND (u.level IS NULL OR UPPER(u.level) <> 'STUDENT');

-- Active Instructor with non-INSTRUCTOR level (must be 0)
SELECT 'active_instructor_level_not_instructor_cnt' AS report, COUNT(*) AS cnt
FROM enrollment e
INNER JOIN `user` u ON u.id = e.user_id
WHERE e.active = 1
  AND e.course_role = 'Instructor'
  AND (u.level IS NULL OR UPPER(u.level) <> 'INSTRUCTOR');

-- Users who are Active TA and also Active Primary Instructor somewhere (informational)
SELECT 'user_active_ta_and_primary_instructor' AS report, u.id AS user_id, u.email, u.level
FROM `user` u
WHERE EXISTS (
        SELECT 1 FROM enrollment e
        WHERE e.user_id = u.id AND e.active = 1 AND e.course_role = 'TA'
      )
  AND EXISTS (
        SELECT 1 FROM enrollment e2
        WHERE e2.user_id = u.id AND e2.active = 1 AND e2.course_role = 'Instructor'
      );

-- Inactive historical TA/Student/Instructor vs current level (report only; do not block)
SELECT 'inactive_enrollment_level_mismatch' AS report, e.id AS enrollment_id, e.course_id, e.user_id,
       e.course_role, e.active, u.role, u.level
FROM enrollment e
INNER JOIN `user` u ON u.id = e.user_id
WHERE e.active = 0
  AND (
        (e.course_role = 'Instructor' AND (u.role <> 'USER' OR UPPER(IFNULL(u.level, '')) <> 'INSTRUCTOR'))
     OR (e.course_role IN ('Student', 'TA') AND (u.role <> 'USER' OR UPPER(IFNULL(u.level, '')) <> 'STUDENT'))
  );
