-- course_part1_gate_check.sql
-- Expect zero problem rows after expand+backfill (+ optional UK tighten).

SELECT 'creator_actor_null' AS gate, COUNT(*) AS bad_cnt
FROM course
WHERE creator_actor_type IS NULL OR creator_actor_id IS NULL OR creator_role IS NULL;

SELECT 'creator_actor_not_user' AS gate, COUNT(*) AS bad_cnt
FROM course
WHERE creator_actor_type <> 'USER';

SELECT 'course_audit_table_missing' AS gate,
       CASE WHEN COUNT(*) = 0 THEN 1 ELSE 0 END AS bad_cnt
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'course_audit_log';

SELECT 'multi_active_instructor' AS gate, COUNT(*) AS bad_cnt
FROM (
  SELECT course_id
  FROM enrollment
  WHERE course_role = 'Instructor' AND active = 1
  GROUP BY course_id
  HAVING COUNT(*) > 1
) d;

-- After UK tighten: generated column expression should mention active
SELECT 'instructor_course_id_expr' AS gate,
       CASE
         WHEN LOWER(generation_expression) LIKE '%active%' THEN 0
         ELSE 1
       END AS bad_cnt
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'enrollment'
  AND column_name = 'instructor_course_id';
