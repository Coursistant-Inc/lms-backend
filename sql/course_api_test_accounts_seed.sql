-- Course API test account seed (lms_v2).
-- Password for all seeded users: Test12345
-- BCrypt (Spring-compatible $2b$): run after tenants exist.
-- Safe to re-run (ON DUPLICATE KEY / email unique).

INSERT INTO tenant (id, name, timezone, status, security_version)
VALUES (1, 'Default', 'America/Los_Angeles', 'ACTIVE', 1)
ON DUPLICATE KEY UPDATE status = 'ACTIVE', timezone = VALUES(timezone);

INSERT INTO tenant (id, name, timezone, status, security_version)
VALUES (2, 'Tenant Two', 'America/New_York', 'ACTIVE', 1)
ON DUPLICATE KEY UPDATE status = 'ACTIVE', timezone = VALUES(timezone);

-- Reuse same hash for all (Test12345)
SET @pwd := '$2b$10$6LAmKnRI48gtN9Sm730ud.53ureihEzi/1FWXc4Pjzl2CIAVxMC3m';

-- instructor2: same tenant as teachtest2 (tenant 1), level INSTRUCTOR
INSERT INTO `user` (tenant_id, username, password, name, email, role, level, status, auth_version, must_change_password, email_notifications)
SELECT 1, 'teachtest3', @pwd, 'Teach Test Three', 'teachtest3@example.com', 'USER', 'INSTRUCTOR', 'ACTIVE', 1, 0, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE email = 'teachtest3@example.com');

UPDATE `user`
SET role = 'USER', level = 'INSTRUCTOR', status = 'ACTIVE', tenant_id = 1, password = @pwd
WHERE email = 'teachtest3@example.com';

-- tenantAdminSame: tenant 1 TENANT_ADMIN
INSERT INTO `user` (tenant_id, username, password, name, email, role, level, status, auth_version, must_change_password, email_notifications)
SELECT 1, 'tenantadmin1', @pwd, 'Tenant Admin One', 'tenantadmin1@example.com', 'TENANT_ADMIN', 'NOT_APPLICABLE', 'ACTIVE', 1, 0, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE email = 'tenantadmin1@example.com');

UPDATE `user`
SET role = 'TENANT_ADMIN', level = 'NOT_APPLICABLE', status = 'ACTIVE', tenant_id = 1, password = @pwd
WHERE email = 'tenantadmin1@example.com';

-- tenantAdminOther: tenant 2 TENANT_ADMIN
INSERT INTO `user` (tenant_id, username, password, name, email, role, level, status, auth_version, must_change_password, email_notifications)
SELECT 2, 'tenantadmin2', @pwd, 'Tenant Admin Two', 'tenantadmin2@example.com', 'TENANT_ADMIN', 'NOT_APPLICABLE', 'ACTIVE', 1, 0, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE email = 'tenantadmin2@example.com');

UPDATE `user`
SET role = 'TENANT_ADMIN', level = 'NOT_APPLICABLE', status = 'ACTIVE', tenant_id = 2, password = @pwd
WHERE email = 'tenantadmin2@example.com';

-- account_identity claims (USER principal)
INSERT INTO account_identity (normalized_email, principal_type, principal_id)
SELECT LOWER(u.email), 'USER', u.id
FROM `user` u
WHERE u.email IN (
  'teachtest3@example.com',
  'tenantadmin1@example.com',
  'tenantadmin2@example.com'
)
AND NOT EXISTS (
  SELECT 1 FROM account_identity ai WHERE ai.normalized_email = LOWER(u.email)
);

-- Verify
SELECT id, email, role, level, tenant_id, status
FROM `user`
WHERE email IN (
  'teachtest2@example.com',
  'teachtest3@example.com',
  'tenantadmin1@example.com',
  'tenantadmin2@example.com',
  'regtest1@example.com',
  'regtest2@example.com',
  'regtest5@example.com'
)
ORDER BY email;
