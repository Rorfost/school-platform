<#
.SYNOPSIS
    Non-destructive verification of Flyway migrations, table counts, and timezone on target DB.
.PARAMETER TargetDbUrl
    Connection string for the target database.
.EXAMPLE
    .\verify_parity.ps1 -TargetDbUrl "postgresql://user:pass@ep-staging.us-east-2.aws.neon.tech/neondb?sslmode=require"
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$TargetDbUrl = $env:TARGET_DB_URL
)

$ErrorActionPreference = "Stop"

if (-not $TargetDbUrl -and -not $env:PGHOST) {
    Write-Error "ERROR: Target database connection string not specified. Set -TargetDbUrl or `$env:TARGET_DB_URL."
    exit 1
}

Write-Host "======================================================================"
Write-Host "         DATABASE PARITY & INTEGRITY VERIFICATION                    "
Write-Host "======================================================================"

Write-Host "==> 1. Checking Database Timezone..."
& psql $TargetDbUrl -X -t -A -c "SHOW timezone;"

Write-Host "==> 2. Checking Flyway Migration History (Last 10 Applied)..."
& psql $TargetDbUrl -X -c "
SELECT installed_rank, version, description, type, script, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 10;
"

Write-Host "==> 3. Verifying Table Row Counts (Core Domain)..."
& psql $TargetDbUrl -X -c "
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

Write-Host "==> 4. Checking Multi-Tenant Foreign Key Integrity..."
& psql $TargetDbUrl -X -c "
SELECT count(*) AS orphan_students 
FROM students s 
LEFT JOIN schools sc ON s.school_id = sc.id 
WHERE sc.id IS NULL;
"

Write-Host "======================================================================"
Write-Host "==> Verification completed successfully."
Write-Host "======================================================================"
