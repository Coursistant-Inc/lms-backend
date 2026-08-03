-- admin_role_phase1_restore.sql
-- Restore admin.role from backup only. Refresh tokens are NOT restored (users re-login).
-- Do NOT restore old JWT signing keys. Keep AUTH_JWT_MIN_ISSUED_AT / new PEM keys.

UPDATE admin a
INNER JOIN admin_role_phase1_admin_backup b ON a.id = b.id
SET a.role = b.old_role;

-- Optional: leave backup tables for audit; drop when confirmed.
-- DROP TABLE IF EXISTS admin_role_phase1_admin_backup;
-- DROP TABLE IF EXISTS admin_role_phase1_refresh_backup;
