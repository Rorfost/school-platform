CREATE TABLE schools (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(80) NOT NULL,
    address TEXT,
    contact_email VARCHAR(254),
    contact_phone VARCHAR(40),
    logo_object_key VARCHAR(512),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT schools_slug_format CHECK (slug = lower(slug) AND slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
    CONSTRAINT schools_name_not_blank CHECK (btrim(name) <> '')
);

CREATE TABLE principal_profiles (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL UNIQUE,
    full_name VARCHAR(160) NOT NULL,
    biography TEXT,
    portrait_object_key VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT principal_profiles_school_fk FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT principal_profiles_name_not_blank CHECK (btrim(full_name) <> '')
);

CREATE TABLE admin_users (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'PRINCIPAL',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT admin_users_school_fk FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT admin_users_school_id_unique UNIQUE (id, school_id),
    CONSTRAINT admin_users_school_email_unique UNIQUE (school_id, email),
    CONSTRAINT admin_users_email_normalized CHECK (email = lower(btrim(email))),
    CONSTRAINT admin_users_role CHECK (role = 'PRINCIPAL'),
    CONSTRAINT admin_users_password_hash_not_blank CHECK (btrim(password_hash) <> '')
);

CREATE TABLE academic_years (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    name VARCHAR(40) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'CURRENT',
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT academic_years_school_fk FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT academic_years_school_id_unique UNIQUE (id, school_id),
    CONSTRAINT academic_years_school_name_unique UNIQUE (school_id, name),
    CONSTRAINT academic_years_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT academic_years_dates_valid CHECK (ends_on > starts_on),
    CONSTRAINT academic_years_status_valid CHECK (status IN ('CURRENT', 'ARCHIVED')),
    CONSTRAINT academic_years_archive_timestamp_valid CHECK (
        (status = 'CURRENT' AND archived_at IS NULL)
        OR (status = 'ARCHIVED' AND archived_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX academic_years_one_current_per_school
    ON academic_years (school_id)
    WHERE status = 'CURRENT';

CREATE TABLE standards (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    code VARCHAR(20) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    sort_order SMALLINT NOT NULL,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT standards_school_fk FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT standards_school_id_unique UNIQUE (id, school_id),
    CONSTRAINT standards_school_code_unique UNIQUE (school_id, code),
    CONSTRAINT standards_code_not_blank CHECK (btrim(code) <> ''),
    CONSTRAINT standards_display_name_not_blank CHECK (btrim(display_name) <> ''),
    CONSTRAINT standards_sort_order_positive CHECK (sort_order > 0)
);

CREATE TABLE subjects (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(120) NOT NULL,
    sort_order SMALLINT NOT NULL,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT subjects_school_fk FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT subjects_school_id_unique UNIQUE (id, school_id),
    CONSTRAINT subjects_school_code_unique UNIQUE (school_id, code),
    CONSTRAINT subjects_code_normalized CHECK (code = upper(btrim(code))),
    CONSTRAINT subjects_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT subjects_sort_order_positive CHECK (sort_order > 0)
);

CREATE TABLE standard_subjects (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    standard_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT standard_subjects_standard_fk FOREIGN KEY (standard_id, school_id)
        REFERENCES standards (id, school_id) ON DELETE RESTRICT,
    CONSTRAINT standard_subjects_subject_fk FOREIGN KEY (subject_id, school_id)
        REFERENCES subjects (id, school_id) ON DELETE RESTRICT,
    CONSTRAINT standard_subjects_school_id_unique UNIQUE (id, school_id),
    CONSTRAINT standard_subjects_school_standard_unique UNIQUE (id, school_id, standard_id),
    CONSTRAINT standard_subjects_standard_subject_unique UNIQUE (standard_id, subject_id)
);

CREATE TABLE students (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    standard_id UUID NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    roll_number VARCHAR(32) NOT NULL,
    result_pin_hash VARCHAR(255) NOT NULL,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT students_school_fk FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT students_academic_year_fk FOREIGN KEY (academic_year_id, school_id)
        REFERENCES academic_years (id, school_id) ON DELETE RESTRICT,
    CONSTRAINT students_standard_fk FOREIGN KEY (standard_id, school_id)
        REFERENCES standards (id, school_id) ON DELETE RESTRICT,
    CONSTRAINT students_school_year_standard_unique UNIQUE (id, school_id, academic_year_id, standard_id),
    CONSTRAINT students_enrollment_roll_unique UNIQUE (school_id, academic_year_id, standard_id, roll_number),
    CONSTRAINT students_name_not_blank CHECK (btrim(full_name) <> ''),
    CONSTRAINT students_roll_number_not_blank CHECK (btrim(roll_number) <> ''),
    CONSTRAINT students_result_pin_hash_bcrypt CHECK (result_pin_hash ~ '^\$2[aby]\$.{56}$')
);

CREATE TABLE assessment_types (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    sort_order SMALLINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT assessment_types_code_normalized CHECK (code = upper(btrim(code))),
    CONSTRAINT assessment_types_display_name_not_blank CHECK (btrim(display_name) <> ''),
    CONSTRAINT assessment_types_sort_order_positive CHECK (sort_order > 0)
);

CREATE TABLE assessments (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    standard_id UUID NOT NULL,
    assessment_type_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT assessments_school_fk FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT assessments_academic_year_fk FOREIGN KEY (academic_year_id, school_id)
        REFERENCES academic_years (id, school_id) ON DELETE RESTRICT,
    CONSTRAINT assessments_standard_fk FOREIGN KEY (standard_id, school_id)
        REFERENCES standards (id, school_id) ON DELETE RESTRICT,
    CONSTRAINT assessments_type_fk FOREIGN KEY (assessment_type_id) REFERENCES assessment_types (id) ON DELETE RESTRICT,
    CONSTRAINT assessments_scope_unique UNIQUE (id, school_id, academic_year_id, standard_id),
    CONSTRAINT assessments_school_standard_unique UNIQUE (id, school_id, standard_id),
    CONSTRAINT assessments_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT assessments_status_valid CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT assessments_lifecycle_valid CHECK (
        (status = 'DRAFT' AND published_at IS NULL AND archived_at IS NULL)
        OR (status = 'PUBLISHED' AND published_at IS NOT NULL AND archived_at IS NULL)
        OR (status = 'ARCHIVED' AND archived_at IS NOT NULL)
    )
);

CREATE TABLE assessment_subjects (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    assessment_id UUID NOT NULL,
    standard_id UUID NOT NULL,
    standard_subject_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT assessment_subjects_assessment_fk FOREIGN KEY (assessment_id, school_id, standard_id)
        REFERENCES assessments (id, school_id, standard_id) ON DELETE RESTRICT,
    CONSTRAINT assessment_subjects_standard_subject_fk FOREIGN KEY (standard_subject_id, school_id, standard_id)
        REFERENCES standard_subjects (id, school_id, standard_id) ON DELETE RESTRICT,
    CONSTRAINT assessment_subjects_scope_unique UNIQUE (id, assessment_id, school_id),
    CONSTRAINT assessment_subjects_assessment_subject_unique UNIQUE (assessment_id, standard_subject_id)
);

CREATE TABLE marks (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    standard_id UUID NOT NULL,
    assessment_id UUID NOT NULL,
    student_id UUID NOT NULL,
    assessment_subject_id UUID NOT NULL,
    score NUMERIC(7, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT marks_assessment_scope_fk FOREIGN KEY (assessment_id, school_id, academic_year_id, standard_id)
        REFERENCES assessments (id, school_id, academic_year_id, standard_id) ON DELETE RESTRICT,
    CONSTRAINT marks_student_scope_fk FOREIGN KEY (student_id, school_id, academic_year_id, standard_id)
        REFERENCES students (id, school_id, academic_year_id, standard_id) ON DELETE RESTRICT,
    CONSTRAINT marks_assessment_subject_scope_fk FOREIGN KEY (assessment_subject_id, assessment_id, school_id)
        REFERENCES assessment_subjects (id, assessment_id, school_id) ON DELETE RESTRICT,
    CONSTRAINT marks_student_assessment_subject_unique UNIQUE (student_id, assessment_id, assessment_subject_id),
    CONSTRAINT marks_score_non_negative CHECK (score >= 0)
);

CREATE TABLE study_materials (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    academic_year_id UUID,
    standard_subject_id UUID,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    storage_bucket VARCHAR(120) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    byte_size BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT study_materials_school_fk FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT study_materials_academic_year_fk FOREIGN KEY (academic_year_id, school_id)
        REFERENCES academic_years (id, school_id) ON DELETE RESTRICT,
    CONSTRAINT study_materials_standard_subject_fk FOREIGN KEY (standard_subject_id, school_id)
        REFERENCES standard_subjects (id, school_id) ON DELETE RESTRICT,
    CONSTRAINT study_materials_object_unique UNIQUE (storage_bucket, object_key),
    CONSTRAINT study_materials_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT study_materials_storage_not_blank CHECK (btrim(storage_bucket) <> '' AND btrim(object_key) <> ''),
    CONSTRAINT study_materials_filename_not_blank CHECK (btrim(original_filename) <> ''),
    CONSTRAINT study_materials_content_type_not_blank CHECK (btrim(content_type) <> ''),
    CONSTRAINT study_materials_byte_size_positive CHECK (byte_size > 0),
    CONSTRAINT study_materials_checksum_format CHECK (checksum_sha256 IS NULL OR checksum_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT study_materials_status_valid CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT study_materials_lifecycle_valid CHECK (
        (status = 'DRAFT' AND published_at IS NULL AND archived_at IS NULL)
        OR (status = 'PUBLISHED' AND published_at IS NOT NULL AND archived_at IS NULL)
        OR (status = 'ARCHIVED' AND archived_at IS NOT NULL)
    )
);

CREATE TABLE notices (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    body TEXT NOT NULL,
    attachment_bucket VARCHAR(120),
    attachment_object_key VARCHAR(512),
    attachment_filename VARCHAR(255),
    attachment_content_type VARCHAR(120),
    attachment_byte_size BIGINT,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notices_school_fk FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT notices_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT notices_body_not_blank CHECK (btrim(body) <> ''),
    CONSTRAINT notices_attachment_complete CHECK (
        (attachment_bucket IS NULL AND attachment_object_key IS NULL AND attachment_filename IS NULL
            AND attachment_content_type IS NULL AND attachment_byte_size IS NULL)
        OR (attachment_bucket IS NOT NULL AND attachment_object_key IS NOT NULL AND attachment_filename IS NOT NULL
            AND attachment_content_type IS NOT NULL AND attachment_byte_size > 0)
    ),
    CONSTRAINT notices_status_valid CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT notices_lifecycle_valid CHECK (
        (status = 'DRAFT' AND published_at IS NULL AND archived_at IS NULL)
        OR (status = 'PUBLISHED' AND published_at IS NOT NULL AND archived_at IS NULL)
        OR (status = 'ARCHIVED' AND archived_at IS NOT NULL)
    )
);

CREATE TABLE gallery_albums (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT gallery_albums_school_fk FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT gallery_albums_school_id_unique UNIQUE (id, school_id),
    CONSTRAINT gallery_albums_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT gallery_albums_status_valid CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT gallery_albums_lifecycle_valid CHECK (
        (status = 'DRAFT' AND published_at IS NULL AND archived_at IS NULL)
        OR (status = 'PUBLISHED' AND published_at IS NOT NULL AND archived_at IS NULL)
        OR (status = 'ARCHIVED' AND archived_at IS NOT NULL)
    )
);

CREATE TABLE gallery_images (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    gallery_album_id UUID NOT NULL,
    storage_bucket VARCHAR(120) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    byte_size BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64),
    alt_text VARCHAR(255) NOT NULL,
    caption TEXT,
    sort_order INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT gallery_images_album_fk FOREIGN KEY (gallery_album_id, school_id)
        REFERENCES gallery_albums (id, school_id) ON DELETE CASCADE,
    CONSTRAINT gallery_images_object_unique UNIQUE (storage_bucket, object_key),
    CONSTRAINT gallery_images_album_sort_order_unique UNIQUE (gallery_album_id, sort_order),
    CONSTRAINT gallery_images_storage_not_blank CHECK (btrim(storage_bucket) <> '' AND btrim(object_key) <> ''),
    CONSTRAINT gallery_images_filename_not_blank CHECK (btrim(original_filename) <> ''),
    CONSTRAINT gallery_images_content_type_not_blank CHECK (btrim(content_type) <> ''),
    CONSTRAINT gallery_images_byte_size_positive CHECK (byte_size > 0),
    CONSTRAINT gallery_images_checksum_format CHECK (checksum_sha256 IS NULL OR checksum_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT gallery_images_alt_text_not_blank CHECK (btrim(alt_text) <> ''),
    CONSTRAINT gallery_images_sort_order_non_negative CHECK (sort_order >= 0),
    CONSTRAINT gallery_images_status_valid CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT gallery_images_lifecycle_valid CHECK (
        (status = 'DRAFT' AND published_at IS NULL AND archived_at IS NULL)
        OR (status = 'PUBLISHED' AND published_at IS NOT NULL AND archived_at IS NULL)
        OR (status = 'ARCHIVED' AND archived_at IS NOT NULL)
    )
);

CREATE TABLE downloads (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    academic_year_id UUID,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    category VARCHAR(60),
    storage_bucket VARCHAR(120) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    byte_size BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT downloads_school_fk FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT downloads_academic_year_fk FOREIGN KEY (academic_year_id, school_id)
        REFERENCES academic_years (id, school_id) ON DELETE RESTRICT,
    CONSTRAINT downloads_object_unique UNIQUE (storage_bucket, object_key),
    CONSTRAINT downloads_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT downloads_storage_not_blank CHECK (btrim(storage_bucket) <> '' AND btrim(object_key) <> ''),
    CONSTRAINT downloads_filename_not_blank CHECK (btrim(original_filename) <> ''),
    CONSTRAINT downloads_content_type_not_blank CHECK (btrim(content_type) <> ''),
    CONSTRAINT downloads_byte_size_positive CHECK (byte_size > 0),
    CONSTRAINT downloads_checksum_format CHECK (checksum_sha256 IS NULL OR checksum_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT downloads_status_valid CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT downloads_lifecycle_valid CHECK (
        (status = 'DRAFT' AND published_at IS NULL AND archived_at IS NULL)
        OR (status = 'PUBLISHED' AND published_at IS NOT NULL AND archived_at IS NULL)
        OR (status = 'ARCHIVED' AND archived_at IS NOT NULL)
    )
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    actor_admin_user_id UUID,
    action VARCHAR(120) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id UUID,
    request_id VARCHAR(128),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT audit_logs_school_fk FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT audit_logs_actor_fk FOREIGN KEY (actor_admin_user_id, school_id)
        REFERENCES admin_users (id, school_id) ON DELETE SET NULL (actor_admin_user_id),
    CONSTRAINT audit_logs_action_not_blank CHECK (btrim(action) <> ''),
    CONSTRAINT audit_logs_target_type_not_blank CHECK (btrim(target_type) <> ''),
    CONSTRAINT audit_logs_metadata_object CHECK (jsonb_typeof(metadata) = 'object')
);

CREATE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER schools_set_updated_at BEFORE UPDATE ON schools FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER principal_profiles_set_updated_at BEFORE UPDATE ON principal_profiles FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER admin_users_set_updated_at BEFORE UPDATE ON admin_users FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER academic_years_set_updated_at BEFORE UPDATE ON academic_years FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER standards_set_updated_at BEFORE UPDATE ON standards FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER subjects_set_updated_at BEFORE UPDATE ON subjects FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER standard_subjects_set_updated_at BEFORE UPDATE ON standard_subjects FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER students_set_updated_at BEFORE UPDATE ON students FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER assessments_set_updated_at BEFORE UPDATE ON assessments FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER assessment_subjects_set_updated_at BEFORE UPDATE ON assessment_subjects FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER marks_set_updated_at BEFORE UPDATE ON marks FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER study_materials_set_updated_at BEFORE UPDATE ON study_materials FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER notices_set_updated_at BEFORE UPDATE ON notices FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER gallery_albums_set_updated_at BEFORE UPDATE ON gallery_albums FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER gallery_images_set_updated_at BEFORE UPDATE ON gallery_images FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER downloads_set_updated_at BEFORE UPDATE ON downloads FOR EACH ROW EXECUTE FUNCTION set_updated_at();
