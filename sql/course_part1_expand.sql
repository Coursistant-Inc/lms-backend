-- course_part1_expand.sql
-- Additive only. Safe before backfill. Does not drop creator_id.

ALTER TABLE course
  ADD COLUMN creator_actor_type VARCHAR(16) NULL AFTER creator_id,
  ADD COLUMN creator_actor_id INT NULL AFTER creator_actor_type,
  ADD COLUMN creator_role VARCHAR(32) NULL AFTER creator_actor_id;

CREATE TABLE IF NOT EXISTS course_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  tenant_id INT NULL,
  actor_type VARCHAR(16) NOT NULL,
  actor_id INT NOT NULL,
  actor_role VARCHAR(32) NULL,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(32) NULL,
  target_id INT NULL,
  before_json JSON NULL,
  after_json JSON NULL,
  request_id VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_course_audit_course (course_id),
  KEY idx_course_audit_actor (actor_type, actor_id),
  KEY idx_course_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
