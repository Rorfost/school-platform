-- Allow admin to customise the 'Parinam Patrak' title shown on the result sheet.
ALTER TABLE result_presentation_settings
    ADD COLUMN result_sheet_title VARCHAR(200);
