ALTER TABLE schools
    ADD CONSTRAINT schools_school_code_not_blank
        CHECK (school_code IS NULL OR btrim(school_code) <> '');

CREATE INDEX students_school_standard_roll_idx
    ON students (school_id, standard_id, roll_number);
COMMENT ON INDEX students_school_standard_roll_idx IS
    'Supports standard-wise student roster queries and roll number lookups.';

CREATE INDEX study_materials_school_standard_subject_idx
    ON study_materials (school_id, standard_subject_id)
    WHERE status = 'PUBLISHED';
COMMENT ON INDEX study_materials_school_standard_subject_idx IS
    'Supports efficient standard-subject filtered study material retrieval.';
