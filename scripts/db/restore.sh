#!/usr/bin/env bash
# ==============================================================================
# Script: restore.sh
# Purpose: Restore a custom format pg_dump export into a target PostgreSQL database
# Usage: ./scripts/db/restore.sh <DUMP_FILE> [TARGET_DB_URL]
# Requirements:
#   - TARGET_DB_URL or PG* standard environment variables set.
#   - pg_restore installed.
# Safety:
#   - By default requires confirmation if TARGET_DB_URL contains "prod".
# ==============================================================================
set -euo pipefail

if [[ $# -lt 1 ]]; then
    echo "Usage: ./scripts/db/restore.sh <dump_file_path> [target_db_url]" >&2
    exit 1
fi

DUMP_FILE="$1"
TARGET_URL="${2:-${TARGET_DB_URL:-}}"

if [[ ! -f "$DUMP_FILE" ]]; then
    echo "ERROR: Backup file does not exist: $DUMP_FILE" >&2
    exit 1
fi

if [[ -z "$TARGET_URL" && -z "${PGHOST:-}" ]]; then
    echo "ERROR: Target database not specified. Set TARGET_DB_URL or supply as argument 2." >&2
    exit 1
fi

# Guard against destructive overwrite without explicit operator confirmation
if [[ "${CONFIRM_DESTRUCTIVE_RESTORE:-0}" != "1" ]]; then
    echo "======================================================================" >&2
    echo "SAFETY ERROR: Restore is DESTRUCTIVE (--clean drops existing objects)." >&2
    echo "Target DB: ${TARGET_URL}" >&2
    echo "To execute, you must set CONFIRM_DESTRUCTIVE_RESTORE=1." >&2
    echo "======================================================================" >&2
    exit 1
fi

# Additional guard if target URL contains "prod"
if [[ "$TARGET_URL" =~ prod ]] && [[ "${ALLOW_PROD_RESTORE:-0}" != "1" ]]; then
    echo "SAFETY ERROR: Target URL contains 'prod'. Aborting to prevent accidental overwrite." >&2
    echo "Set ALLOW_PROD_RESTORE=1 in addition to CONFIRM_DESTRUCTIVE_RESTORE=1 to override." >&2
    exit 1
fi

echo "==> Restoring from ${DUMP_FILE}..."

RESTORE_ARGS=(
    --clean
    --if-exists
    --no-owner
    --no-acl
)

set +e
if [[ -n "$TARGET_URL" ]]; then
    pg_restore "${RESTORE_ARGS[@]}" --dbname="$TARGET_URL" "$DUMP_FILE"
    STATUS=$?
else
    pg_restore "${RESTORE_ARGS[@]}" "$DUMP_FILE"
    STATUS=$?
fi
set -e

if [[ $STATUS -eq 0 ]]; then
    echo "==> Database restore finished cleanly (exit code 0)."
elif [[ $STATUS -eq 1 && "${ACCEPT_RESTORE_WARNINGS:-0}" == "1" ]]; then
    echo "==> WARNING: pg_restore exited with code 1 (warnings/errors present)."
    echo "==> ACCEPT_RESTORE_WARNINGS=1 was supplied by operator. Proceeding."
else
    echo "ERROR: pg_restore failed with exit code $STATUS." >&2
    echo "Nonzero exit codes are treated as failures. If pg_restore produced only acceptable" >&2
    echo "clean drops on non-existent tables and you have reviewed the log, set ACCEPT_RESTORE_WARNINGS=1." >&2
    exit $STATUS
fi

echo "==> Run scripts/db/verify_parity.sh to validate table counts and schema integrity."
