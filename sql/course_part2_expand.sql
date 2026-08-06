-- course_part2_expand.sql
-- Additive archive actor columns for Part 2 lifecycle.

ALTER TABLE course
  ADD COLUMN archived_by_actor_type VARCHAR(16) NULL AFTER archived_at,
  ADD COLUMN archived_by_actor_id INT NULL AFTER archived_by_actor_type;
