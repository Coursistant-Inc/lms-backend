-- course_part3_gate_check.sql
-- Layered gate: structure (fail) + active invariant (fail) + legacy incomplete (report only).

-- A) Structure: columns
SELECT 'missing_withdrawn_columns' AS check_name, COUNT(*) AS cnt
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'enrollment'
  AND column_name IN ('withdrawn_at', 'withdrawn_by_actor_type', 'withdrawn_by_actor_id');
-- Expect cnt = 3

-- A) Structure: index
SELECT 'missing_idx_enrollment_course_active' AS check_name, COUNT(*) AS cnt
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'enrollment'
  AND index_name = 'idx_enrollment_course_active';
-- Expect cnt >= 1

-- B) Active invariant (FAIL if cnt > 0): active rows must have all withdrawn_* NULL
SELECT 'active_with_withdrawn_dirty' AS check_name, COUNT(*) AS cnt
FROM enrollment
WHERE active = 1
  AND (withdrawn_at IS NOT NULL
       OR withdrawn_by_actor_type IS NOT NULL
       OR withdrawn_by_actor_id IS NOT NULL);

-- C) Legacy incomplete inactive (REPORT ONLY, do not fail gate)
SELECT 'legacy_incomplete_withdrawn_count' AS check_name, COUNT(*) AS cnt
FROM enrollment
WHERE active = 0
  AND (withdrawn_at IS NULL
       OR withdrawn_by_actor_type IS NULL
       OR withdrawn_by_actor_id IS NULL);
