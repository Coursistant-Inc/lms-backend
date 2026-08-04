-- auth_refresh_rotation_expand.sql
-- Expand-only: add stable session rotation columns. Safe before cutover cleanup.

ALTER TABLE refresh_tokens
  ADD COLUMN IF NOT EXISTS session_id VARCHAR(64) NULL AFTER id,
  ADD COLUMN IF NOT EXISTS previous_token VARCHAR(128) NULL AFTER token,
  ADD COLUMN IF NOT EXISTS previous_valid_until DATETIME NULL AFTER previous_token;

-- Unique session_id for rows that have one (new sessions after cutover).
CREATE UNIQUE INDEX IF NOT EXISTS uk_refresh_tokens_session_id ON refresh_tokens (session_id);

-- Deployment cutover (maintenance window): revoke all existing refresh sessions.
-- Reuse admin role cutover window if running together; do not backfill session_id.
-- DELETE FROM refresh_tokens;
-- Also flush Redis db used for refresh keys (refresh:token:* / refresh:used:*).
