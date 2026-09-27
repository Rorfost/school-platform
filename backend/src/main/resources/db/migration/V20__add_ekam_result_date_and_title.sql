-- Separate result date and sheet title for Ekam Kasoti results.
ALTER TABLE result_presentation_settings
    ADD COLUMN ekam_result_date   DATE          NULL,
    ADD COLUMN ekam_result_sheet_title VARCHAR(200) NULL;
