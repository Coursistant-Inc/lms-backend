-- Drop legacy Quiz tables if they exist (no data migration).
-- Manual execution only — do not run on application startup.
-- Safe for public tables: does not touch course / user / enrollment.

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS quiz_audit_log;
DROP TABLE IF EXISTS quiz_score_audit;
DROP TABLE IF EXISTS quiz_grade;
DROP TABLE IF EXISTS quiz_attempt_answer;
DROP TABLE IF EXISTS quiz_attempt;
DROP TABLE IF EXISTS quiz_question_option;
DROP TABLE IF EXISTS quiz_question;
DROP TABLE IF EXISTS quiz;

-- Legacy names that may have existed in older schemas
DROP TABLE IF EXISTS quiz_answer;
DROP TABLE IF EXISTS quiz_option;
DROP TABLE IF EXISTS quiz_submission;
DROP TABLE IF EXISTS quiz_result;
DROP TABLE IF EXISTS quiz_grade_release;

SET FOREIGN_KEY_CHECKS = 1;
