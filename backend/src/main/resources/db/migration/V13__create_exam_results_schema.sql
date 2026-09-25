CREATE TABLE annual_exam_results (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    student_name VARCHAR(255) NOT NULL,
    standard VARCHAR(50) NOT NULL,
    roll_number INTEGER NOT NULL,
    general_register_number VARCHAR(100),
    birth_date VARCHAR(100),
    total_working_days INTEGER,
    attended_days INTEGER,
    total_marks INTEGER,
    obtained_marks INTEGER,
    percentage NUMERIC(5, 2),
    overall_grade VARCHAR(10),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX annual_exam_results_lookup_idx ON annual_exam_results(school_id, academic_year_id, standard, roll_number);

CREATE TABLE annual_exam_result_subjects (
    id UUID PRIMARY KEY,
    exam_result_id UUID NOT NULL REFERENCES annual_exam_results(id) ON DELETE CASCADE,
    subject_name VARCHAR(100) NOT NULL,
    maximum_marks INTEGER NOT NULL,
    obtained_marks INTEGER,
    grade VARCHAR(10),
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX annual_exam_result_subjects_exam_idx ON annual_exam_result_subjects(exam_result_id);
