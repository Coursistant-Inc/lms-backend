-- admin_identity_phase2_precheck.sql
SELECT 'dup_email_across_tables' AS check_name, LOWER(TRIM(a.email)) AS email
FROM admin a
INNER JOIN `user` u ON LOWER(TRIM(a.email)) = LOWER(TRIM(u.email));

SELECT 'admin_null_email' AS check_name, id FROM admin WHERE email IS NULL OR TRIM(email) = '';
SELECT 'user_null_email' AS check_name, id FROM `user` WHERE email IS NULL OR TRIM(email) = '';
-- Abort migration if any rows returned above.
