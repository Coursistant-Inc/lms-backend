-- Tenant table for lms_v2 (MySQL) — Phase 1 Admin CRUD
-- No FK from course.tenant_id yet; delete-time reference check is application-level.

CREATE TABLE IF NOT EXISTS tenant (
  id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  timezone VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed aligns with CourseService.DEFAULT_TENANT_ID = 1 and lms.institution-timezone
INSERT INTO tenant (id, name, timezone)
VALUES (1, 'Default', 'America/Los_Angeles')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  timezone = VALUES(timezone);

-- Keep auto-increment above seed id when table was empty
ALTER TABLE tenant AUTO_INCREMENT = 2;
