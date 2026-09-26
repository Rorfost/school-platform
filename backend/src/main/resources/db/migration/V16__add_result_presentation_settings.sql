ALTER TABLE principal_profiles ADD COLUMN signature_object_key VARCHAR(512);

ALTER TABLE standards
    ADD COLUMN class_teacher_name VARCHAR(160),
    ADD COLUMN class_teacher_signature_object_key VARCHAR(512);

ALTER TABLE standard_subjects
    ADD COLUMN maximum_marks INTEGER NOT NULL DEFAULT 100,
    ADD CONSTRAINT standard_subjects_maximum_marks_positive CHECK (maximum_marks > 0);

CREATE TABLE result_presentation_settings (
    school_id UUID PRIMARY KEY REFERENCES schools(id) ON DELETE CASCADE,
    result_date DATE,
    footer_line_one TEXT,
    footer_line_two TEXT,
    grade_a_min NUMERIC(5,2) NOT NULL DEFAULT 80,
    grade_b_min NUMERIC(5,2) NOT NULL DEFAULT 65,
    grade_c_min NUMERIC(5,2) NOT NULL DEFAULT 50,
    grade_d_min NUMERIC(5,2) NOT NULL DEFAULT 35,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
