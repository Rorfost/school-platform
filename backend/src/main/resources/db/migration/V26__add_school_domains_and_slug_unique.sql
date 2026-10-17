-- V26__add_school_domains_and_slug_unique.sql
-- Enforces slug uniqueness on schools and establishes the school_domains table
-- for operator domain mapping and multi-tenant operational consistency checks.

-- 1. Ensure slug is uniquely indexed on schools table
CREATE UNIQUE INDEX schools_slug_unique
    ON schools (slug);

COMMENT ON INDEX schools_slug_unique IS
    'Guarantees unique route slug across all tenant schools.';

-- 2. Create school_domains table for operational domain mapping
CREATE TABLE school_domains (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    hostname VARCHAR(255) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT school_domains_school_fk
        FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT school_domains_hostname_format
        CHECK (hostname = lower(btrim(hostname)) AND length(hostname) >= 3),
    CONSTRAINT school_domains_hostname_unique
        UNIQUE (hostname)
);

CREATE INDEX school_domains_school_id_idx
    ON school_domains (school_id);

COMMENT ON TABLE school_domains IS
    'Maps operator-configured hostnames/domains to schools for multi-tenant route consistency.';
COMMENT ON COLUMN school_domains.hostname IS
    'Normalized lowercase unique hostname (e.g. school-b.workers.dev or custom domain).';
