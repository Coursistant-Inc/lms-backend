-- Phase 0c: enrollment flag for Student→TA promote submission freeze (Part 8)

ALTER TABLE enrollment
  ADD COLUMN assignment_submit_frozen TINYINT(1) NOT NULL DEFAULT 0
  AFTER active;
