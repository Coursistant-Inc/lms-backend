-- Course Part 4: upload_operation + minio_object_outbox (additive)

CREATE TABLE IF NOT EXISTS upload_operation (
  id CHAR(36) NOT NULL,
  actor_type VARCHAR(16) NOT NULL,
  actor_id INT NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  route_id VARCHAR(255) NOT NULL,
  fingerprint VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL,
  visibility_status VARCHAR(16) NOT NULL,
  course_id INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_upload_op_actor_key_route (actor_type, actor_id, idempotency_key, route_id),
  KEY idx_upload_op_course (course_id),
  KEY idx_upload_op_status (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS minio_object_outbox (
  id BIGINT NOT NULL AUTO_INCREMENT,
  upload_operation_id CHAR(36) NULL,
  bucket VARCHAR(128) NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  action VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lease_until DATETIME NULL,
  last_error VARCHAR(512) NULL,
  course_id INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  active_dedupe_key VARCHAR(700)
    GENERATED ALWAYS AS (
      CASE
        WHEN status IN ('PENDING', 'IN_PROGRESS')
          THEN CONCAT(bucket, CHAR(0), object_key, CHAR(0), action)
        ELSE NULL
      END
    ) STORED,
  PRIMARY KEY (id),
  UNIQUE KEY uk_minio_outbox_active (active_dedupe_key),
  KEY idx_minio_outbox_claim (status, next_retry_at, lease_until),
  KEY idx_minio_outbox_op (upload_operation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- visibility_status on course_material (skip if already applied)
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'course_material'
    AND COLUMN_NAME = 'visibility_status'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE course_material ADD COLUMN visibility_status VARCHAR(16) NOT NULL DEFAULT ''READY''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
