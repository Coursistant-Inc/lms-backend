-- admin_role_phase1.sql
-- Idempotent ADMIN → SYSTEM_ADMIN cutover. Clears all sessions.
-- Prerequisite: admin_role_phase1_precheck.sql passed; app traffic stopped.
-- Does NOT rewrite historical TA levels (precheck must fail if TA exists).

-- Backup tables (idempotent recreate for this maintenance window)
DROP TABLE IF EXISTS admin_role_phase1_admin_backup;
CREATE TABLE admin_role_phase1_admin_backup AS
SELECT id, role AS old_role, NOW() AS backed_up_at
FROM admin
WHERE role = 'ADMIN';

DROP TABLE IF EXISTS admin_role_phase1_refresh_backup;
CREATE TABLE admin_role_phase1_refresh_backup AS
SELECT id, user_id, role, token, expire_time, NOW() AS backed_up_at
FROM refresh_tokens;

UPDATE admin
SET role = 'SYSTEM_ADMIN'
WHERE role = 'ADMIN';

-- Force re-login for every principal
DELETE FROM refresh_tokens;

-- Note: Redis keys refresh:token:*, refresh:used:*, user:active:*, auth:principal:*
-- must be FLUSHED or selectively deleted by ops after this SQL (see runbook).
