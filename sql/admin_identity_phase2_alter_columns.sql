-- Prefer running each ALTER separately; ignore duplicate-column errors if already applied.

ALTER TABLE `user` ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE `user` ADD COLUMN auth_version INT NOT NULL DEFAULT 1;
ALTER TABLE `user` ADD COLUMN temporary_password_expires_at DATETIME NULL;

ALTER TABLE admin ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE admin ADD COLUMN auth_version INT NOT NULL DEFAULT 1;

ALTER TABLE tenant ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE tenant ADD COLUMN security_version INT NOT NULL DEFAULT 1;
