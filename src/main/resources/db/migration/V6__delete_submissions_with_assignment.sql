ALTER TABLE nw_submission 
DROP FOREIGN KEY fk_nw_submission_assignment;

ALTER TABLE nw_submission
ADD CONSTRAINT fk_nw_submission_assignment 
FOREIGN KEY (assignment_id) 
REFERENCES nw_assignment(id) 
ON DELETE CASCADE;