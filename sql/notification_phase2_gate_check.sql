-- Gate check after notification_phase2.sql.
-- 4 version columns must exist, be NOT NULL, default 0, and contain no NULL/negative values.

SELECT
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'assignment'
      AND column_name = 'publication_version'
      AND is_nullable = 'NO'
      AND column_default = '0') AS assignment_publication_version,
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'assignment'
      AND column_name = 'schedule_version'
      AND is_nullable = 'NO'
      AND column_default = '0') AS assignment_schedule_version,
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'quiz'
      AND column_name = 'publication_version'
      AND is_nullable = 'NO'
      AND column_default = '0') AS quiz_publication_version,
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'course_week'
      AND column_name = 'publication_version'
      AND is_nullable = 'NO'
      AND column_default = '0') AS course_week_publication_version,
  (SELECT COUNT(*) FROM assignment
    WHERE publication_version IS NULL OR publication_version < 0
       OR schedule_version IS NULL OR schedule_version < 0) AS assignment_invalid_versions,
  (SELECT COUNT(*) FROM quiz
    WHERE publication_version IS NULL OR publication_version < 0) AS quiz_invalid_versions,
  (SELECT COUNT(*) FROM course_week
    WHERE publication_version IS NULL OR publication_version < 0) AS course_week_invalid_versions;
