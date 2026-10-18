-- V28__drop_student_uid_from_annual_exam_results.sql
-- Removes student_uid column to comply with privacy policy prohibiting government/state student identifiers.
-- Data-removal impact: drops any previously stored student_uid values from annual_exam_results.

ALTER TABLE annual_exam_results
    DROP COLUMN IF EXISTS student_uid;
