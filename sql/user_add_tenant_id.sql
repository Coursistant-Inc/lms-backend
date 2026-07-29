-- Add user.tenant_id (NOT NULL). Non-idempotent — run once.
-- Prerequisite: tenant.id = 1 must exist.
--   SELECT id FROM tenant WHERE id = 1;

ALTER TABLE `user`
  ADD COLUMN tenant_id INT NULL AFTER id;

UPDATE `user`
SET tenant_id = 1;

ALTER TABLE `user`
  MODIFY COLUMN tenant_id INT NOT NULL;

ALTER TABLE `user`
  ADD KEY idx_user_tenant (tenant_id);
