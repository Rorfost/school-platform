CREATE INDEX students_school_year_standard_name_idx
    ON students (school_id, academic_year_id, standard_id, full_name)
    WHERE is_archived = FALSE;
COMMENT ON INDEX students_school_year_standard_name_idx IS
    'Supports administrative student lists and name lookup within an academic scope.';

CREATE UNIQUE INDEX subjects_school_normalized_name_unique
    ON subjects (school_id, lower(name));
COMMENT ON INDEX subjects_school_normalized_name_unique IS
    'Prevents visually duplicate subject names that differ only by case.';

CREATE INDEX assessments_public_lookup_idx
    ON assessments (school_id, academic_year_id, standard_id, published_at DESC)
    WHERE status = 'PUBLISHED';
COMMENT ON INDEX assessments_public_lookup_idx IS
    'Supports published-assessment selection for individual result lookup.';

CREATE INDEX marks_assessment_student_idx
    ON marks (assessment_id, student_id);
COMMENT ON INDEX marks_assessment_student_idx IS
    'Supports loading one student result without scanning a class result set.';

CREATE INDEX study_materials_published_filter_idx
    ON study_materials (school_id, academic_year_id, standard_subject_id, published_at DESC)
    WHERE status = 'PUBLISHED';
COMMENT ON INDEX study_materials_published_filter_idx IS
    'Supports public material filtering by academic year and mapped subject.';

CREATE INDEX notices_published_list_idx
    ON notices (school_id, published_at DESC)
    WHERE status = 'PUBLISHED';
COMMENT ON INDEX notices_published_list_idx IS
    'Supports the public notice listing in publication order.';

CREATE INDEX gallery_albums_published_list_idx
    ON gallery_albums (school_id, published_at DESC)
    WHERE status = 'PUBLISHED';
COMMENT ON INDEX gallery_albums_published_list_idx IS
    'Supports public gallery album ordering.';

CREATE INDEX gallery_images_published_order_idx
    ON gallery_images (gallery_album_id, sort_order)
    WHERE status = 'PUBLISHED';
COMMENT ON INDEX gallery_images_published_order_idx IS
    'Supports ordered rendering of publicly visible album images.';

CREATE INDEX downloads_published_filter_idx
    ON downloads (school_id, academic_year_id, category, published_at DESC)
    WHERE status = 'PUBLISHED';
COMMENT ON INDEX downloads_published_filter_idx IS
    'Supports public download and timetable filtering.';

CREATE INDEX audit_logs_school_created_at_idx
    ON audit_logs (school_id, created_at DESC);
COMMENT ON INDEX audit_logs_school_created_at_idx IS
    'Supports recent administrative audit review without indexing audit metadata.';
