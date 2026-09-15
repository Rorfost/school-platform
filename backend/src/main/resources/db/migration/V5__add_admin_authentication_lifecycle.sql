ALTER TABLE admin_users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE admin_users
    ADD CONSTRAINT admin_users_password_hash_bcrypt
    CHECK (password_hash ~ '^\$2[aby]\$.{56}$');

CREATE INDEX admin_users_active_email_idx
    ON admin_users (email)
    WHERE is_active = TRUE;
COMMENT ON INDEX admin_users_active_email_idx IS
    'Supports the single-school principal login lookup without indexing inactive accounts.';
