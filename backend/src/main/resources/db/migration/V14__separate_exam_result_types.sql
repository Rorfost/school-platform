ALTER TABLE annual_exam_results
    ADD COLUMN result_type VARCHAR(20) NOT NULL DEFAULT 'ANNUAL',
    ADD CONSTRAINT annual_exam_results_type_check
        CHECK (result_type IN ('ANNUAL', 'EKAM_KASOTI'));

DROP INDEX annual_exam_results_lookup_idx;

CREATE INDEX annual_exam_results_lookup_idx
    ON annual_exam_results (school_id, academic_year_id, result_type, standard, roll_number);
