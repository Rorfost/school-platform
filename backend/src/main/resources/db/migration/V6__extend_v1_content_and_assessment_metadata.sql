ALTER TABLE schools
    ADD COLUMN short_name VARCHAR(80),
    ADD COLUMN school_code VARCHAR(40),
    ADD COLUMN city VARCHAR(100),
    ADD COLUMN state VARCHAR(100),
    ADD COLUMN postal_code VARCHAR(20),
    ADD COLUMN website VARCHAR(255),
    ADD COLUMN maps_url VARCHAR(2048),
    ADD COLUMN about TEXT,
    ADD COLUMN established_year SMALLINT,
    ADD COLUMN medium VARCHAR(80),
    ADD COLUMN school_type VARCHAR(80),
    ADD CONSTRAINT schools_school_code_unique UNIQUE (school_code),
    ADD CONSTRAINT schools_established_year_valid
        CHECK (established_year IS NULL OR established_year BETWEEN 1800 AND 2100),
    ADD CONSTRAINT schools_website_not_blank CHECK (website IS NULL OR btrim(website) <> ''),
    ADD CONSTRAINT schools_maps_url_not_blank CHECK (maps_url IS NULL OR btrim(maps_url) <> '');

ALTER TABLE principal_profiles
    ADD COLUMN qualification VARCHAR(160),
    ADD COLUMN designation VARCHAR(120),
    ADD COLUMN message TEXT,
    ADD COLUMN email VARCHAR(254),
    ADD COLUMN phone VARCHAR(40),
    ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN is_contact_public BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE standard_subjects
    ADD COLUMN sort_order SMALLINT NOT NULL DEFAULT 1,
    ADD CONSTRAINT standard_subjects_sort_order_positive CHECK (sort_order > 0);

ALTER TABLE assessments
    ADD COLUMN description TEXT,
    ADD COLUMN assessment_date DATE;

ALTER TABLE assessment_subjects
    ADD COLUMN maximum_marks NUMERIC(7, 2),
    ADD COLUMN passing_marks NUMERIC(7, 2),
    ADD CONSTRAINT assessment_subjects_marks_valid CHECK (
        (maximum_marks IS NULL AND passing_marks IS NULL)
        OR (maximum_marks > 0 AND passing_marks >= 0 AND passing_marks <= maximum_marks)
    );

ALTER TABLE study_materials
    ADD COLUMN material_type VARCHAR(60) NOT NULL DEFAULT 'DOCUMENT',
    ADD CONSTRAINT study_materials_material_type_not_blank CHECK (btrim(material_type) <> '');

ALTER TABLE notices
    ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN expires_at TIMESTAMPTZ;

ALTER TABLE gallery_albums
    ADD COLUMN cover_image_id UUID;

ALTER TABLE gallery_images
    ADD CONSTRAINT gallery_images_id_album_unique UNIQUE (id, gallery_album_id);

ALTER TABLE gallery_albums
    ADD CONSTRAINT gallery_albums_cover_image_fk FOREIGN KEY (cover_image_id, id)
        REFERENCES gallery_images (id, gallery_album_id) ON DELETE SET NULL (cover_image_id);

CREATE INDEX standard_subjects_school_standard_order_idx
    ON standard_subjects (school_id, standard_id, sort_order);
COMMENT ON INDEX standard_subjects_school_standard_order_idx IS
    'Supports ordered subject selection for a standard.';

CREATE INDEX notices_public_current_list_idx
    ON notices (school_id, is_pinned DESC, published_at DESC)
    WHERE status = 'PUBLISHED';
COMMENT ON INDEX notices_public_current_list_idx IS
    'Supports public pinned-first notice listings; expiry remains a runtime time predicate.';

CREATE INDEX assessments_school_status_updated_idx
    ON assessments (school_id, status, updated_at DESC);
COMMENT ON INDEX assessments_school_status_updated_idx IS
    'Supports principal assessment lists without indexing every assessment field.';
