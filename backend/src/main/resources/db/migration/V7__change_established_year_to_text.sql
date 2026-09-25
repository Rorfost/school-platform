ALTER TABLE schools
  DROP CONSTRAINT IF EXISTS schools_established_year_valid;

ALTER TABLE schools
  ALTER COLUMN established_year TYPE VARCHAR(255);
