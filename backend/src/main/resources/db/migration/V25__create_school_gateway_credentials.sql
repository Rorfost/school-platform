CREATE TABLE school_gateway_credentials (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    token_digest BYTEA NOT NULL,
    label VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    rotated_at TIMESTAMPTZ,
    CONSTRAINT school_gateway_credentials_school_fk
        FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT school_gateway_credentials_label_not_blank
        CHECK (btrim(label) <> ''),
    CONSTRAINT school_gateway_credentials_expiry_valid
        CHECK (expires_at IS NULL OR expires_at > created_at)
);

CREATE UNIQUE INDEX school_gateway_credentials_digest_unique
    ON school_gateway_credentials (token_digest);

CREATE INDEX school_gateway_credentials_school_id_idx
    ON school_gateway_credentials (school_id);

CREATE INDEX school_gateway_credentials_active_idx
    ON school_gateway_credentials (school_id, created_at DESC)
    WHERE revoked_at IS NULL;

COMMENT ON TABLE school_gateway_credentials IS
    'Cryptographic SHA-256 digests of Cloudflare Worker gateway credentials mapped to active schools for multi-tenant route resolution.';
COMMENT ON COLUMN school_gateway_credentials.token_digest IS
    'SHA-256 hash (32 bytes) of the random >=256-bit opaque PORTAL_GATEWAY_TOKEN secret; plaintext is never stored.';
COMMENT ON COLUMN school_gateway_credentials.expires_at IS
    'Optional scheduled expiration time; expired tokens are rejected by gateway resolution.';
COMMENT ON COLUMN school_gateway_credentials.revoked_at IS
    'Immediate revocation timestamp for disabled or rotated credentials.';
COMMENT ON COLUMN school_gateway_credentials.last_used_at IS
    'Audit timestamp recording the most recent successful gateway routing validation.';
COMMENT ON COLUMN school_gateway_credentials.rotated_at IS
    'Timestamp when a replacement credential was issued during planned secret rotation.';
