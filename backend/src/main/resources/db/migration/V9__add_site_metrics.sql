CREATE TABLE site_metrics (
    school_id UUID PRIMARY KEY REFERENCES schools (id) ON DELETE RESTRICT,
    total_visits BIGINT NOT NULL DEFAULT 0 CHECK (total_visits >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
