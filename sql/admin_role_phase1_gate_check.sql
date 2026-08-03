-- admin_role_phase1_gate_check.sql
-- All of these must return zero problem rows / expected counts.

SELECT 'remaining_admin_role' AS gate, COUNT(*) AS bad_cnt
FROM admin WHERE role = 'ADMIN';

SELECT 'non_system_admin_roles' AS gate, COUNT(*) AS bad_cnt
FROM admin WHERE role <> 'SYSTEM_ADMIN';

SELECT 'user_level_ta' AS gate, COUNT(*) AS bad_cnt
FROM `user` WHERE level = 'TA';

SELECT 'tenant_admin_bad' AS gate, COUNT(*) AS bad_cnt
FROM `user`
WHERE role = 'TENANT_ADMIN'
  AND (level <> 'NOT_APPLICABLE' OR tenant_id IS NULL);

SELECT 'user_role_level_bad' AS gate, COUNT(*) AS bad_cnt
FROM `user`
WHERE role = 'USER' AND level NOT IN ('INSTRUCTOR', 'STUDENT');

SELECT 'refresh_tokens_remaining' AS gate, COUNT(*) AS bad_cnt
FROM refresh_tokens;

SELECT 'system_admin_present' AS gate, COUNT(*) AS cnt
FROM admin WHERE role = 'SYSTEM_ADMIN';

-- Expect: first six gates bad_cnt = 0; system_admin_present cnt >= 1
