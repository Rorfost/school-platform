DROP INDEX IF EXISTS annual_exam_results_lookup_idx;

ALTER TABLE annual_exam_results
    DROP COLUMN academic_year_id;

CREATE INDEX annual_exam_results_lookup_idx
    ON annual_exam_results (school_id, result_type, standard, roll_number);
