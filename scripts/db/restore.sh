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

# Guard against accidental restoration to production
if [[ "$TARGET_URL" =~ prod ]] && [[ "${ALLOW_PROD_RESTORE:-0}" != "1" ]]; then
    echo "SAFETY ERROR: Target URL contains 'prod'. Aborting to prevent accidental overwrite." >&2
    echo "Set ALLOW_PROD_RESTORE=1 to override." >&2
    exit 1
fi

echo "==> Restoring from ${DUMP_FILE}..."

RESTORE_ARGS=(
    --clean
    --if-exists
    --no-owner
    --no-acl
)

if [[ -n "$TARGET_URL" ]]; then
    pg_restore "${RESTORE_ARGS[@]}" --dbname="$TARGET_URL" "$DUMP_FILE" || {
        STATUS=$?
        # pg_restore returns exit status 1 if there were non-fatal warnings
        if [[ $STATUS -eq 1 ]]; then
            echo "==> Restore completed with non-fatal warnings (exit code 1)."
        else
            echo "ERROR: pg_restore failed with exit code $STATUS" >&2
            exit $STATUS
        fi
    }
else
    pg_restore "${RESTORE_ARGS[@]}" "$DUMP_FILE" || {
        STATUS=$?
        if [[ $STATUS -eq 1 ]]; then
            echo "==> Restore completed with non-fatal warnings (exit code 1)."
        else
            echo "ERROR: pg_restore failed with exit code $STATUS" >&2
            exit $STATUS
        fi
    }
fi

echo "==> Database restore finished successfully."
echo "==> Run scripts/db/verify_parity.sh to validate table counts and schema integrity."
