ALTER TABLE standard_subjects
    ADD COLUMN maximum_marks_configured BOOLEAN NOT NULL DEFAULT FALSE;

-- Existing values were defaults from the earlier migration, not confirmed curriculum settings.
-- Require the administrator to save each subject maximum before those values are used in a result.
UPDATE standard_subjects
SET maximum_marks_configured = FALSE;
