-- admin_role_phase1_precheck.sql
-- Stop on any unexpected role/level before migration. Does not modify data.
-- Run against the target schema; inspect result sets before continuing.

SELECT 'admin_role_unexpected' AS check_name, id, role
FROM admin
WHERE role NOT IN ('ADMIN', 'SYSTEM_ADMIN');

SELECT 'user_role_unexpected' AS check_name, id, role
FROM `user`
WHERE role IS NULL OR role NOT IN ('USER', 'TENANT_ADMIN');

SELECT 'user_level_ta_or_unexpected' AS check_name, id, role, level, tenant_id
FROM `user`
WHERE level IS NULL
   OR level NOT IN ('INSTRUCTOR', 'STUDENT', 'NOT_APPLICABLE')
   OR level = 'TA';

SELECT 'tenant_admin_invariant_fail' AS check_name, id, role, level, tenant_id
FROM `user`
WHERE role = 'TENANT_ADMIN'
  AND (level <> 'NOT_APPLICABLE' OR tenant_id IS NULL);

SELECT 'user_role_level_mismatch' AS check_name, id, role, level
FROM `user`
WHERE role = 'USER'
  AND level NOT IN ('INSTRUCTOR', 'STUDENT');

SELECT 'admin_count' AS check_name, COUNT(*) AS cnt FROM admin;
SELECT 'system_admin_or_admin_count' AS check_name, COUNT(*) AS cnt
FROM admin WHERE role IN ('ADMIN', 'SYSTEM_ADMIN');

SELECT 'refresh_token_roles' AS check_name, role, COUNT(*) AS cnt
FROM refresh_tokens
GROUP BY role;

-- Manual gate: if any of the first five SELECT blocks return rows, abort migration.
-- Also abort if admin table is empty.
