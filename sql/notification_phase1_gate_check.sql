-- Gate check after notification_phase1.sql. All expected tables/keys must exist.
-- Stale digest items with digest_date < CURRENT_DATE remaining PENDING means Digest is not running.
-- digest_terminal_parent_processing_children and overdue_collecting must be 0 on a healthy node.
-- long_pending_* are visibility counters, not hard fail.

SELECT
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'notification_event_outbox') AS outbox_table,
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'notification_event_recipient') AS recipient_table,
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'notification_delivery') AS delivery_table,
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'notification_digest_email') AS digest_email_table,
  (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'notification_event_outbox'
      AND index_name = 'uk_outbox_event') AS uk_outbox_event,
  (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'notification_delivery'
      AND index_name = 'uk_delivery_dedupe') AS uk_delivery_dedupe,
  (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'notification_digest_email'
      AND index_name = 'uk_digest_email') AS uk_digest_email,
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'notification_delivery'
      AND column_name IN ('claim_token', 'send_attempted_at', 'unknown_outcome_count')) AS delivery_claim_columns,
  (SELECT COUNT(*) FROM notification_delivery
    WHERE channel = 'DAILY_DIGEST' AND status = 'PENDING' AND digest_date < CURRENT_DATE) AS stale_digest_pending,
  (SELECT COUNT(*) FROM notification_digest_email e
    WHERE e.status IN ('SENT', 'DRY_RUN', 'FAILED_PERMANENT', 'SKIPPED_PREFERENCE', 'SKIPPED_INELIGIBLE')
      AND EXISTS (
        SELECT 1 FROM notification_delivery d
        WHERE d.digest_email_id = e.id AND d.status = 'PROCESSING'
      )) AS digest_terminal_parent_processing_children,
  (SELECT COUNT(*) FROM notification_digest_email
    WHERE status = 'COLLECTING'
      AND updated_at < DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 10 MINUTE)) AS overdue_collecting,
  (SELECT COUNT(*) FROM notification_event_outbox
    WHERE status = 'PENDING'
      AND created_at < DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 HOUR)) AS long_pending_outbox,
  (SELECT COUNT(*) FROM notification_delivery
    WHERE status = 'PENDING'
      AND created_at < DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 HOUR)) AS long_pending_delivery;
