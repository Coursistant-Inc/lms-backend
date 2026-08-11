-- course_part1_precheck.sql
-- Read-only gate before Course Part 1 schema changes. Abort migration if any bad_cnt > 0
-- (except informational counts). Does not modify data.

-- 1) Courses with zero Active Instructors
SELECT 'course_zero_active_instructor' AS check_name, c.id AS course_id
FROM course c
LEFT JOIN enrollment e
  ON e.course_id = c.id AND e.course_role = 'Instructor' AND e.active = 1
GROUP BY c.id
HAVING COUNT(e.id) = 0;

-- 2) Courses with more than one Active Instructor
SELECT 'course_multi_active_instructor' AS check_name, c.id AS course_id, COUNT(e.id) AS active_instructor_cnt
FROM course c
INNER JOIN enrollment e
  ON e.course_id = c.id AND e.course_role = 'Instructor' AND e.active = 1
GROUP BY c.id
HAVING COUNT(e.id) > 1;

-- 3) Cross-tenant enrollment (user.tenant_id <> course.tenant_id)
SELECT 'enrollment_cross_tenant' AS check_name, e.id AS enrollment_id, e.course_id, e.user_id,
       c.tenant_id AS course_tenant_id, u.tenant_id AS user_tenant_id
FROM enrollment e
INNER JOIN course c ON c.id = e.course_id
INNER JOIN `user` u ON u.id = e.user_id
WHERE u.tenant_id IS NULL OR c.tenant_id IS NULL OR u.tenant_id <> c.tenant_id;

-- 4) Active Instructor must be USER + INSTRUCTOR (blocking)
SELECT 'active_instructor_role_level_bad' AS check_name, e.id AS enrollment_id, e.course_id, e.user_id,
       u.role, u.level, e.course_role
FROM enrollment e
INNER JOIN `user` u ON u.id = e.user_id
WHERE e.active = 1
  AND e.course_role = 'Instructor'
  AND (u.role <> 'USER' OR u.level <> 'INSTRUCTOR');

-- 5) Active Student/TA must be USER + STUDENT (blocking)
SELECT 'active_student_ta_role_level_bad' AS check_name, e.id AS enrollment_id, e.course_id, e.user_id,
       u.role, u.level, e.course_role
FROM enrollment e
INNER JOIN `user` u ON u.id = e.user_id
WHERE e.active = 1
  AND e.course_role IN ('Student', 'TA')
  AND (u.role <> 'USER' OR u.level <> 'STUDENT');

-- 5b) Active enrollment on non-USER / NOT_APPLICABLE (blocking)
SELECT 'active_enrollment_non_user_account' AS check_name, e.id AS enrollment_id, e.course_id, e.user_id,
       u.role, u.level, e.course_role
FROM enrollment e
INNER JOIN `user` u ON u.id = e.user_id
WHERE e.active = 1
  AND (u.role IS NULL OR u.role <> 'USER' OR u.level = 'NOT_APPLICABLE');

-- 6) Enrollment references missing / disabled user
SELECT 'enrollment_user_missing_or_disabled' AS check_name, e.id AS enrollment_id, e.user_id, u.status
FROM enrollment e
LEFT JOIN `user` u ON u.id = e.user_id
WHERE u.id IS NULL OR u.status IS NULL OR u.status = '' OR u.status = 'DISABLED';

-- 7) Creator cannot map to USER actor (missing user or not USER role)
SELECT 'course_creator_unmappable' AS check_name, c.id AS course_id, c.creator_id, u.role, u.status
FROM course c
LEFT JOIN `user` u ON u.id = c.creator_id
WHERE u.id IS NULL
   OR u.role IS NULL
   OR u.role <> 'USER';

-- Summary counts (informational)
SELECT 'summary_course_cnt' AS check_name, COUNT(*) AS cnt FROM course;
SELECT 'summary_enrollment_cnt' AS check_name, COUNT(*) AS cnt FROM enrollment;
