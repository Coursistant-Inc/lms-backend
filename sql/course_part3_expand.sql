-- course_part3_expand.sql
-- Additive withdrawn actor columns + member-list index. Safe before code deploy.

ALTER TABLE enrollment
  ADD COLUMN withdrawn_at DATETIME NULL AFTER enrolled_at,
  ADD COLUMN withdrawn_by_actor_type VARCHAR(16) NULL AFTER withdrawn_at,
  ADD COLUMN withdrawn_by_actor_id INT NULL AFTER withdrawn_by_actor_type;

ALTER TABLE enrollment
  ADD KEY idx_enrollment_course_active (course_id, active);
