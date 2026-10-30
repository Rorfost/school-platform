-- V27__create_school_features_schema.sql
-- Creates school_features table for per-school feature flag overrides.

CREATE TABLE school_features (
    school_id UUID NOT NULL,
    feature_key VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT school_features_pk
        PRIMARY KEY (school_id, feature_key),
    CONSTRAINT school_features_school_fk
        FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT school_features_key_not_blank
        CHECK (btrim(feature_key) <> '')
);

CREATE INDEX school_features_school_id_idx
    ON school_features (school_id);

COMMENT ON TABLE school_features IS
    'Per-school feature flag toggles that override the default platform feature policy.';
COMMENT ON COLUMN school_features.feature_key IS
    'Uppercase identifier matching FeatureKey enum (e.g. GALLERY, MATERIALS, NOTICES, DOWNLOADS, EXAM_RESULTS).';
COMMENT ON COLUMN school_features.enabled IS
    'Explicit enablement override for the school.';
