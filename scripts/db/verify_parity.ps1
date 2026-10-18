<#
.SYNOPSIS
    Non-destructive parity comparison of Flyway migrations, table counts, UTC timezone,
    and multi-tenant foreign key integrity between source and target PostgreSQL databases.
.PARAMETER SourceDbUrl
    Connection string for the source database (or reads from $env:SOURCE_DB_URL).
.PARAMETER TargetDbUrl
    Connection string for the target database (or reads from $env:TARGET_DB_URL).
.EXAMPLE
    .\verify_parity.ps1 -SourceDbUrl "postgresql://user:pass@host1/db1" -TargetDbUrl "postgresql://user:pass@host2/db2"
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$SourceDbUrl = $env:SOURCE_DB_URL,

    [Parameter(Position = 1)]
    [string]$TargetDbUrl = $env:TARGET_DB_URL
)

$ErrorActionPreference = "Stop"

if (-not $SourceDbUrl -or -not $TargetDbUrl) {
    Write-Error "ERROR: Both -SourceDbUrl and -TargetDbUrl (or `$env:SOURCE_DB_URL and `$env:TARGET_DB_URL) are required."
    exit 1
}

$Failures = 0

Write-Host "======================================================================"
Write-Host "         DATABASE PARITY & INTEGRITY COMPARISON                       "
Write-Host "======================================================================"

# ------------------------------------------------------------------------------
# 1. Check Timezones (both must be UTC)
# ------------------------------------------------------------------------------
Write-Host "==> 1. Checking Database Timezones (must be UTC)..."
$SrcTz = (& psql $SourceDbUrl -X -t -A -c "SHOW timezone;").Trim()
$TgtTz = (& psql $TargetDbUrl -X -t -A -c "SHOW timezone;").Trim()

Write-Host "    Source Timezone : $SrcTz"
Write-Host "    Target Timezone : $TgtTz"

if ($SrcTz -ne "UTC" -and $SrcTz -ne "Etc/UTC") {
    Write-Host "    [FAIL] Source database timezone is '$SrcTz', expected 'UTC'." -ForegroundColor Red
    $Failures++
}

if ($TgtTz -ne "UTC" -and $TgtTz -ne "Etc/UTC") {
    Write-Host "    [FAIL] Target database timezone is '$TgtTz', expected 'UTC'." -ForegroundColor Red
    $Failures++
}

if ($SrcTz -eq $TgtTz -and ($SrcTz -eq "UTC" -or $SrcTz -eq "Etc/UTC")) {
    Write-Host "    [PASS] Timezones match and are set to UTC." -ForegroundColor Green
}

# ------------------------------------------------------------------------------
# 2. Compare Flyway Migration History & Checksums
# ------------------------------------------------------------------------------
Write-Host "==> 2. Comparing Flyway Migration History and Checksums..."
$FlywayQuery = "SELECT version || ':' || COALESCE(checksum::text, 'null') || ':' || success::text FROM flyway_schema_history ORDER BY installed_rank;"

$SrcFlyway = (& psql $SourceDbUrl -X -t -A -c $FlywayQuery) | Where-Object { $_ -match '\S' }
$TgtFlyway = (& psql $TargetDbUrl -X -t -A -c $FlywayQuery) | Where-Object { $_ -match '\S' }

Write-Host ("    Source applied migrations : {0}" -f ($SrcFlyway.Count))
Write-Host ("    Target applied migrations : {0}" -f ($TgtFlyway.Count))

$SrcStr = ($SrcFlyway -join "`n").Trim()
$TgtStr = ($TgtFlyway -join "`n").Trim()

if ($SrcStr -ne $TgtStr) {
    Write-Host "    [FAIL] Flyway migration history or checksums differ between source and target!" -ForegroundColor Red
    $Failures++
} else {
    Write-Host "    [PASS] Flyway migrations and checksums match identically." -ForegroundColor Green
}

# ------------------------------------------------------------------------------
# 3. Comparing Table Row Counts
# ------------------------------------------------------------------------------
Write-Host "==> 3. Comparing Row Counts for Shared Tables..."

$Tables = @(
    "schools",
    "principal_profiles",
    "admin_users",
    "academic_years",
    "standards",
    "standard_classes",
    "subjects",
    "standard_subjects",
    "students",
    "assessments",
    "assessment_subjects",
    "marks",
    "annual_exam_results",
    "trimasik_exam_results",
    "study_materials",
    "notices",
    "gallery_albums",
    "gallery_images",
    "downloads",
    "school_gateway_credentials",
    "school_domains",
    "school_features"
)

$QueryParts = @()
foreach ($tbl in $Tables) {
    $QueryParts += "SELECT '$tbl' AS tbl, count(*) AS cnt FROM $tbl"
}
$CountQuery = ($QueryParts -join " UNION ALL ") + " ORDER BY tbl;"

$SrcCountsRaw = (& psql $SourceDbUrl -X -t -A -c $CountQuery) | Where-Object { $_ -match '\|' }
$TgtCountsRaw = (& psql $TargetDbUrl -X -t -A -c $CountQuery) | Where-Object { $_ -match '\|' }

$SrcMap = @{}
foreach ($line in $SrcCountsRaw) {
    $p = $line.Split('|')
    $SrcMap[$p[0].Trim()] = $p[1].Trim()
}

$TgtMap = @{}
foreach ($line in $TgtCountsRaw) {
    $p = $line.Split('|')
    $TgtMap[$p[0].Trim()] = $p[1].Trim()
}

foreach ($tbl in $Tables) {
    $sCnt = $SrcMap[$tbl]
    $tCnt = $TgtMap[$tbl]
    if ($null -ne $sCnt -and $sCnt -eq $tCnt) {
        Write-Host ("    {0,-28} : {1,8} == {2,-8} [PASS]" -f $tbl, $sCnt, $tCnt) -ForegroundColor Green
    } else {
        Write-Host ("    {0,-28} : {1,8} != {2,-8} [FAIL]" -f $tbl, $sCnt, $tCnt) -ForegroundColor Red
        $Failures++
    }
}

# ------------------------------------------------------------------------------
# 4. Multi-Tenant Foreign Key Integrity Check (Orphan checks on target)
# ------------------------------------------------------------------------------
Write-Host "==> 4. Checking Multi-Tenant Foreign Key Integrity on Target..."

$FkChecks = @(
    @{ Label = "students.school_id -> schools.id"; Query = "SELECT count(*) FROM students s LEFT JOIN schools sc ON s.school_id = sc.id WHERE sc.id IS NULL;" },
    @{ Label = "principal_profiles.school_id -> schools.id"; Query = "SELECT count(*) FROM principal_profiles p LEFT JOIN schools sc ON p.school_id = sc.id WHERE sc.id IS NULL;" },
    @{ Label = "admin_users.school_id -> schools.id"; Query = "SELECT count(*) FROM admin_users a LEFT JOIN schools sc ON a.school_id = sc.id WHERE sc.id IS NULL;" },
    @{ Label = "academic_years.school_id -> schools.id"; Query = "SELECT count(*) FROM academic_years ay LEFT JOIN schools sc ON ay.school_id = sc.id WHERE sc.id IS NULL;" },
    @{ Label = "standards.school_id -> schools.id"; Query = "SELECT count(*) FROM standards st LEFT JOIN schools sc ON st.school_id = sc.id WHERE sc.id IS NULL;" },
    @{ Label = "subjects.school_id -> schools.id"; Query = "SELECT count(*) FROM subjects su LEFT JOIN schools sc ON su.school_id = sc.id WHERE sc.id IS NULL;" },
    @{ Label = "assessments.school_id -> schools.id"; Query = "SELECT count(*) FROM assessments a LEFT JOIN schools sc ON a.school_id = sc.id WHERE sc.id IS NULL;" },
    @{ Label = "annual_exam_results.school_id -> schools.id"; Query = "SELECT count(*) FROM annual_exam_results r LEFT JOIN schools sc ON r.school_id = sc.id WHERE sc.id IS NULL;" },
    @{ Label = "school_gateway_credentials.school_id -> schools.id"; Query = "SELECT count(*) FROM school_gateway_credentials c LEFT JOIN schools sc ON c.school_id = sc.id WHERE sc.id IS NULL;" },
    @{ Label = "school_domains.school_id -> schools.id"; Query = "SELECT count(*) FROM school_domains d LEFT JOIN schools sc ON d.school_id = sc.id WHERE sc.id IS NULL;" },
    @{ Label = "school_features.school_id -> schools.id"; Query = "SELECT count(*) FROM school_features f LEFT JOIN schools sc ON f.school_id = sc.id WHERE sc.id IS NULL;" }
)

foreach ($chk in $FkChecks) {
    $res = (& psql $TargetDbUrl -X -t -A -c $chk.Query).Trim()
    if ($res -eq "0") {
        Write-Host ("    {0,-52} : 0 orphans [PASS]" -f $chk.Label) -ForegroundColor Green
    } else {
        Write-Host ("    {0,-52} : {1} orphans [FAIL]" -f $chk.Label, $res) -ForegroundColor Red
        $Failures++
    }
}

Write-Host "======================================================================"
if ($Failures -eq 0) {
    Write-Host "==> ALL PARITY AND INTEGRITY CHECKS PASSED SUCCESSFULLY." -ForegroundColor Green
    Write-Host "======================================================================"
    exit 0
} else {
    Write-Error ("==> PARITY VERIFICATION FAILED: {0} check(s) failed." -f $Failures)
    Write-Host "======================================================================"
    exit 1
}
