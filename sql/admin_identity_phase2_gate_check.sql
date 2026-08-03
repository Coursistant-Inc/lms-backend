-- admin_identity_phase2_gate_check.sql
SELECT 'identity_count' AS gate, COUNT(*) AS cnt FROM account_identity;
SELECT 'dup_normalized_email' AS gate, normalized_email, COUNT(*) AS c
FROM account_identity GROUP BY normalized_email HAVING c > 1;
SELECT 'user_missing_status' AS gate, COUNT(*) AS bad FROM `user` WHERE status IS NULL OR status = '';
SELECT 'admin_missing_status' AS gate, COUNT(*) AS bad FROM admin WHERE status IS NULL OR status = '';
-- Expect no dup rows; status bad = 0
