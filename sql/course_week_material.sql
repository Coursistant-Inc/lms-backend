-- CourseWeek + CourseMaterial for lms_v2

CREATE TABLE IF NOT EXISTS course_week (
  id INT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  order_position INT NOT NULL DEFAULT 0,
  state ENUM('Draft', 'Published') NOT NULL DEFAULT 'Draft',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_course_week_course (course_id),
  KEY idx_course_week_course_order (course_id, order_position),
  CONSTRAINT fk_course_week_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course_material (
  id INT NOT NULL AUTO_INCREMENT,
  week_id INT NOT NULL,
  course_id INT NOT NULL,
  material_type ENUM('FILE', 'LINK') NOT NULL,
  display_name VARCHAR(255) NOT NULL,
  order_position INT NOT NULL DEFAULT 0,
  original_filename VARCHAR(255) NULL,
  content_type VARCHAR(128) NULL,
  extension VARCHAR(32) NULL,
  size_bytes BIGINT NULL,
  object_key VARCHAR(512) NULL,
  link_url VARCHAR(2048) NULL,
  uploaded_by INT NOT NULL,
  visibility_status VARCHAR(16) NOT NULL DEFAULT 'READY',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_course_material_week (week_id),
  KEY idx_course_material_week_order (week_id, order_position),
  KEY idx_course_material_course (course_id),
  KEY idx_course_material_uploader (uploaded_by),
  CONSTRAINT fk_course_material_week FOREIGN KEY (week_id) REFERENCES course_week (id) ON DELETE RESTRICT,
  CONSTRAINT fk_course_material_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE RESTRICT,
  CONSTRAINT fk_course_material_uploader FOREIGN KEY (uploaded_by) REFERENCES user (id) ON DELETE RESTRICT,
  CONSTRAINT chk_course_material_type CHECK (
    (material_type = 'FILE' AND object_key IS NOT NULL AND link_url IS NULL)
    OR (material_type = 'LINK' AND link_url IS NOT NULL AND object_key IS NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Part 4 storage helpers (also in sql/course_part4_expand.sql for additive upgrades)
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
  UNIQUE KEY uk_upload_op_actor_key_route (actor_type, actor_id, idempotency_key, route_id)
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
  KEY idx_minio_outbox_claim (status, next_retry_at, lease_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
