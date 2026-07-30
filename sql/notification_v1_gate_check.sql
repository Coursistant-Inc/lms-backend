-- Gate checks after running notification_v1.sql
-- Expect: orphan_remaining=0, uk_notification_dedupe present, legacy event_key on old rows

SELECT COUNT(*) AS orphan_remaining FROM user_notification WHERE tenant_id IS NULL;

SELECT COUNT(*) AS uk_present
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'user_notification'
  AND index_name = 'uk_notification_dedupe';

SELECT COUNT(*) AS legacy_keys
FROM user_notification
WHERE event_key LIKE 'legacy:%';
