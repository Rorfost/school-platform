#!/usr/bin/env bash
# ==============================================================================
# Script: verify_parity.sh
# Purpose: Non-destructive verification of Flyway migrations, table counts,
#          and database timezone on a target PostgreSQL instance.
# Usage: ./scripts/db/verify_parity.sh [TARGET_DB_URL]
# ==============================================================================
set -euo pipefail

TARGET_URL="${1:-${TARGET_DB_URL:-}}"

if [[ -z "$TARGET_URL" && -z "${PGHOST:-}" ]]; then
    echo "ERROR: Target database URL not provided. Pass as arg 1 or set TARGET_DB_URL." >&2
    exit 1
fi

PSQL_CMD="psql"
if [[ -n "$TARGET_URL" ]]; then
    PSQL_CMD="psql $TARGET_URL"
fi

echo "======================================================================"
echo "         DATABASE PARITY & INTEGRITY VERIFICATION                    "
echo "======================================================================"

echo "==> 1. Checking Database Timezone..."
$PSQL_CMD -X -t -A -c "SHOW timezone;"

echo "==> 2. Checking Flyway Migration History (Last 10 Applied)..."
$PSQL_CMD -X -c "
SELECT installed_rank, version, description, type, script, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 10;
"

echo "==> 3. Verifying Table Row Counts (Core Domain)..."
$PSQL_CMD -X -c "
SELECT 
    'schools' AS table_name, count(*) AS row_count FROM schools
UNION ALL
SELECT 'principal_profiles', count(*) FROM principal_profiles
UNION ALL
SELECT 'academic_years', count(*) FROM academic_years
UNION ALL
SELECT 'standards', count(*) FROM standards
UNION ALL
SELECT 'standard_classes', count(*) FROM standard_classes
UNION ALL
SELECT 'subjects', count(*) FROM subjects
UNION ALL
SELECT 'standard_subjects', count(*) FROM standard_subjects
UNION ALL
SELECT 'students', count(*) FROM students
UNION ALL
SELECT 'assessments', count(*) FROM assessments
UNION ALL
SELECT 'assessment_subjects', count(*) FROM assessment_subjects
UNION ALL
SELECT 'marks', count(*) FROM marks
UNION ALL
SELECT 'study_materials', count(*) FROM study_materials
UNION ALL
SELECT 'notices', count(*) FROM notices
UNION ALL
SELECT 'gallery_albums', count(*) FROM gallery_albums
UNION ALL
SELECT 'gallery_images', count(*) FROM gallery_images
UNION ALL
SELECT 'downloads', count(*) FROM downloads
UNION ALL
SELECT 'school_gateway_credentials', count(*) FROM school_gateway_credentials
UNION ALL
SELECT 'school_domains', count(*) FROM school_domains
UNION ALL
SELECT 'school_features', count(*) FROM school_features
ORDER BY table_name;
"

echo "==> 4. Checking Multi-Tenant Foreign Key Integrity..."
$PSQL_CMD -X -c "
SELECT count(*) AS orphan_students 
FROM students s 
LEFT JOIN schools sc ON s.school_id = sc.id 
WHERE sc.id IS NULL;
"

echo "======================================================================"
echo "==> Verification completed successfully."
echo "======================================================================"
