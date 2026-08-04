-- auth_refresh_rotation_gate_check.sql
-- Run after expand + cutover. Expect zero rows lacking session_id for active tokens.

SELECT COUNT(*) AS missing_session_id
FROM refresh_tokens
WHERE session_id IS NULL OR session_id = '';

SELECT COUNT(*) AS duplicate_session_id
FROM (
  SELECT session_id
  FROM refresh_tokens
  WHERE session_id IS NOT NULL AND session_id <> ''
  GROUP BY session_id
  HAVING COUNT(*) > 1
) d;
