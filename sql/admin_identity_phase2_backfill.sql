-- admin_identity_phase2_backfill.sql (re-entrant)
INSERT IGNORE INTO account_identity (normalized_email, principal_type, principal_id)
SELECT LOWER(TRIM(email)), 'ADMIN', id FROM admin WHERE email IS NOT NULL AND TRIM(email) <> '';

INSERT IGNORE INTO account_identity (normalized_email, principal_type, principal_id)
SELECT LOWER(TRIM(email)), 'USER', id FROM `user` WHERE email IS NOT NULL AND TRIM(email) <> '';

UPDATE `user` SET status = 'ACTIVE' WHERE status IS NULL OR status = '';
UPDATE `user` SET auth_version = 1 WHERE auth_version IS NULL OR auth_version < 1;
UPDATE admin SET status = 'ACTIVE' WHERE status IS NULL OR status = '';
UPDATE admin SET auth_version = 1 WHERE auth_version IS NULL OR auth_version < 1;
UPDATE tenant SET status = 'ACTIVE' WHERE status IS NULL OR status = '';
UPDATE tenant SET security_version = 1 WHERE security_version IS NULL OR security_version < 1;
