#!/usr/bin/env bash
# ==============================================================================
# Script: verify_parity.sh
# Purpose: Non-destructive verification comparing source and target PostgreSQL
#          instances: row counts, Flyway migration checksums, UTC timezone,
#          and multi-tenant foreign key integrity.
# Usage: ./scripts/db/verify_parity.sh <SOURCE_DB_URL> <TARGET_DB_URL>
#        or set SOURCE_DB_URL and TARGET_DB_URL in environment.
# Safety:
#   - Read-only operations only.
#   - Never outputs student records, PII, or credential secrets.
#   - Exits with a nonzero exit code on any parity or integrity mismatch.
# ==============================================================================
set -euo pipefail

SOURCE_URL="${1:-${SOURCE_DB_URL:-}}"
TARGET_URL="${2:-${TARGET_DB_URL:-}}"

if [[ -z "$SOURCE_URL" || -z "$TARGET_URL" ]]; then
    echo "ERROR: Both source and target database URLs are required." >&2
    echo "Usage: ./scripts/db/verify_parity.sh <source_db_url> <target_db_url>" >&2
    echo "   or: SOURCE_DB_URL=... TARGET_DB_URL=... ./scripts/db/verify_parity.sh" >&2
    exit 1
fi

FAILURES=0

echo "======================================================================"
echo "         DATABASE PARITY & INTEGRITY COMPARISON                       "
echo "======================================================================"

# ------------------------------------------------------------------------------
# 1. Check Timezones (both must be UTC)
# ------------------------------------------------------------------------------
echo "==> 1. Checking Database Timezones (must be UTC)..."
SRC_TZ=$(psql "$SOURCE_URL" -X -t -A -c "SHOW timezone;" 2>/dev/null | tr -d '\r')
TGT_TZ=$(psql "$TARGET_URL" -X -t -A -c "SHOW timezone;" 2>/dev/null | tr -d '\r')

echo "    Source Timezone : $SRC_TZ"
echo "    Target Timezone : $TGT_TZ"

if [[ "$SRC_TZ" != "UTC" && "$SRC_TZ" != "Etc/UTC" ]]; then
    echo "    [FAIL] Source database timezone is '$SRC_TZ', expected 'UTC'." >&2
    FAILURES=$((FAILURES + 1))
fi

if [[ "$TGT_TZ" != "UTC" && "$TGT_TZ" != "Etc/UTC" ]]; then
    echo "    [FAIL] Target database timezone is '$TGT_TZ', expected 'UTC'." >&2
    FAILURES=$((FAILURES + 1))
fi

if [[ "$SRC_TZ" == "$TGT_TZ" && ("$SRC_TZ" == "UTC" || "$SRC_TZ" == "Etc/UTC") ]]; then
    echo "    [PASS] Timezones match and are set to UTC."
fi

# ------------------------------------------------------------------------------
# 2. Check Flyway Migration History & Checksums
# ------------------------------------------------------------------------------
echo "==> 2. Comparing Flyway Migration History and Checksums..."
FLYWAY_QUERY="SELECT version || ':' || COALESCE(checksum::text, 'null') || ':' || success::text FROM flyway_schema_history ORDER BY installed_rank;"

SRC_FLYWAY=$(psql "$SOURCE_URL" -X -t -A -c "$FLYWAY_QUERY" 2>/dev/null | tr -d '\r')
TGT_FLYWAY=$(psql "$TARGET_URL" -X -t -A -c "$FLYWAY_QUERY" 2>/dev/null | tr -d '\r')

SRC_MIGRATION_COUNT=$(echo "$SRC_FLYWAY" | grep -c . || true)
TGT_MIGRATION_COUNT=$(echo "$TGT_FLYWAY" | grep -c . || true)

echo "    Source applied migrations : $SRC_MIGRATION_COUNT"
echo "    Target applied migrations : $TGT_MIGRATION_COUNT"

if [[ "$SRC_FLYWAY" != "$TGT_FLYWAY" ]]; then
    echo "    [FAIL] Flyway migration history or checksums differ between source and target!" >&2
    FAILURES=$((FAILURES + 1))
else
    echo "    [PASS] Flyway migrations and checksums match identically."
fi

# ------------------------------------------------------------------------------
# 3. Comparing Table Row Counts Across Core Domain Tables
# ------------------------------------------------------------------------------
echo "==> 3. Comparing Row Counts for Shared Tables..."

TABLES=(
    "schools"
    "principal_profiles"
    "admin_users"
    "academic_years"
    "standards"
    "standard_classes"
    "subjects"
    "standard_subjects"
    "students"
    "assessments"
    "assessment_subjects"
    "marks"
    "annual_exam_results"
    "trimasik_exam_results"
    "study_materials"
    "notices"
    "gallery_albums"
    "gallery_images"
    "downloads"
    "school_gateway_credentials"
    "school_domains"
    "school_features"
)

COUNT_QUERY=""
for tbl in "${TABLES[@]}"; do
    if [[ -z "$COUNT_QUERY" ]]; then
        COUNT_QUERY="SELECT '$tbl' AS tbl, count(*) AS cnt FROM $tbl"
    else
        COUNT_QUERY="$COUNT_QUERY UNION ALL SELECT '$tbl', count(*) FROM $tbl"
    fi
done
COUNT_QUERY="$COUNT_QUERY ORDER BY tbl;"

SRC_COUNTS=$(psql "$SOURCE_URL" -X -t -A -c "$COUNT_QUERY" 2>/dev/null | tr -d '\r')
TGT_COUNTS=$(psql "$TARGET_URL" -X -t -A -c "$COUNT_QUERY" 2>/dev/null | tr -d '\r')

while IFS='|' read -r tbl src_cnt; do
    tgt_cnt=$(echo "$TGT_COUNTS" | grep "^${tbl}|" | cut -d'|' -f2 || echo "missing")
    if [[ "$src_cnt" == "$tgt_cnt" ]]; then
        printf "    %-28s : %8s == %-8s [PASS]\n" "$tbl" "$src_cnt" "$tgt_cnt"
    else
        printf "    %-28s : %8s != %-8s [FAIL]\n" "$tbl" "$src_cnt" "$tgt_cnt" >&2
        FAILURES=$((FAILURES + 1))
    fi
done <<< "$SRC_COUNTS"

# ------------------------------------------------------------------------------
# 4. Multi-Tenant Foreign Key Integrity Check (Orphan checks on target)
# ------------------------------------------------------------------------------
echo "==> 4. Checking Multi-Tenant Foreign Key Integrity on Target..."

FK_CHECKS=(
    "students.school_id -> schools.id|SELECT count(*) FROM students s LEFT JOIN schools sc ON s.school_id = sc.id WHERE sc.id IS NULL;"
    "principal_profiles.school_id -> schools.id|SELECT count(*) FROM principal_profiles p LEFT JOIN schools sc ON p.school_id = sc.id WHERE sc.id IS NULL;"
    "admin_users.school_id -> schools.id|SELECT count(*) FROM admin_users a LEFT JOIN schools sc ON a.school_id = sc.id WHERE sc.id IS NULL;"
    "academic_years.school_id -> schools.id|SELECT count(*) FROM academic_years ay LEFT JOIN schools sc ON ay.school_id = sc.id WHERE sc.id IS NULL;"
    "standards.school_id -> schools.id|SELECT count(*) FROM standards st LEFT JOIN schools sc ON st.school_id = sc.id WHERE sc.id IS NULL;"
    "subjects.school_id -> schools.id|SELECT count(*) FROM subjects su LEFT JOIN schools sc ON su.school_id = sc.id WHERE sc.id IS NULL;"
    "assessments.school_id -> schools.id|SELECT count(*) FROM assessments a LEFT JOIN schools sc ON a.school_id = sc.id WHERE sc.id IS NULL;"
    "annual_exam_results.school_id -> schools.id|SELECT count(*) FROM annual_exam_results r LEFT JOIN schools sc ON r.school_id = sc.id WHERE sc.id IS NULL;"
    "school_gateway_credentials.school_id -> schools.id|SELECT count(*) FROM school_gateway_credentials c LEFT JOIN schools sc ON c.school_id = sc.id WHERE sc.id IS NULL;"
    "school_domains.school_id -> schools.id|SELECT count(*) FROM school_domains d LEFT JOIN schools sc ON d.school_id = sc.id WHERE sc.id IS NULL;"
    "school_features.school_id -> schools.id|SELECT count(*) FROM school_features f LEFT JOIN schools sc ON f.school_id = sc.id WHERE sc.id IS NULL;"
)

for check in "${FK_CHECKS[@]}"; do
    label="${check%%|*}"
    query="${check##*|}"
    orphan_cnt=$(psql "$TARGET_URL" -X -t -A -c "$query" 2>/dev/null | tr -d '\r' || echo "ERR")
    if [[ "$orphan_cnt" == "0" ]]; then
        printf "    %-52s : 0 orphans [PASS]\n" "$label"
    else
        printf "    %-52s : %s orphans [FAIL]\n" "$label" "$orphan_cnt" >&2
        FAILURES=$((FAILURES + 1))
    fi
done

echo "======================================================================"
if [[ $FAILURES -eq 0 ]]; then
    echo "==> ALL PARITY AND INTEGRITY CHECKS PASSED SUCCESSFULLY."
    echo "======================================================================"
    exit 0
else
    echo "==> PARITY VERIFICATION FAILED: $FAILURES check(s) failed." >&2
    echo "======================================================================" >&2
    exit 1
fi
