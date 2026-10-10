CREATE TABLE standard_classes (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    standard_id UUID NOT NULL REFERENCES standards(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    sort_order SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT standard_classes_standard_name_unique UNIQUE (standard_id, name)
);

CREATE INDEX standard_classes_standard_idx ON standard_classes(standard_id);
CREATE INDEX standard_classes_school_idx ON standard_classes(school_id);

ALTER TABLE annual_exam_results
    ADD COLUMN student_uid VARCHAR(100),
    ADD COLUMN student_class VARCHAR(50),
    ALTER COLUMN total_working_days TYPE VARCHAR(50) USING total_working_days::text,
    ALTER COLUMN attended_days TYPE VARCHAR(50) USING attended_days::text;

DROP INDEX IF EXISTS annual_exam_results_lookup_idx;

CREATE INDEX annual_exam_results_lookup_idx
    ON annual_exam_results (school_id, result_type, standard, student_class, roll_number);
