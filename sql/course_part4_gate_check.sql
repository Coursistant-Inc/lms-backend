-- Course Part 4 gate checks

SELECT
  (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'upload_operation') AS upload_operation_exists,
  (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'minio_object_outbox') AS minio_outbox_exists,
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_material'
      AND COLUMN_NAME = 'visibility_status') AS material_visibility_exists,
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'minio_object_outbox'
      AND COLUMN_NAME = 'active_dedupe_key') AS outbox_dedupe_exists;
