-- V29__add_grade_bands_to_result_presentation_settings.sql
-- Adds flexible grade_bands JSONB column to support school-scoped grade label configurations
-- (e.g., A+, A, B, C, D, F) while preserving legacy fixed grade threshold columns for backward compatibility.

ALTER TABLE result_presentation_settings
    ADD COLUMN IF NOT EXISTS grade_bands JSONB DEFAULT '[{"label":"A","minPercentage":80},{"label":"B","minPercentage":65},{"label":"C","minPercentage":50},{"label":"D","minPercentage":35},{"label":"F","minPercentage":0}]'::jsonb;
