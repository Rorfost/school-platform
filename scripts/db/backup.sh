#!/usr/bin/env bash
# ==============================================================================
# Script: backup.sh
# Purpose: Export a PostgreSQL database using pg_dump custom format (-Fc)
# Usage: ./scripts/db/backup.sh [OUTPUT_FILE]
# Requirements:
#   - SOURCE_DB_URL or PG* standard environment variables set.
#   - pg_dump installed.
# Security:
#   - Strips ownership and ACLs (--no-owner --no-acl).
#   - Never outputs credentials or unencrypted data to repo files.
#   - Encrypt output immediately using gpg / openssl if stored externally.
# ==============================================================================
set -euo pipefail

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DEFAULT_BACKUP_DIR="${BACKUP_DIR:-./backups}"
DEFAULT_OUT="${DEFAULT_BACKUP_DIR}/school_portal_${TIMESTAMP}.dump"
OUTPUT_FILE="${1:-$DEFAULT_OUT}"

mkdir -p "$(dirname "$OUTPUT_FILE")"

if [[ -z "${SOURCE_DB_URL:-}" && -z "${PGHOST:-}" ]]; then
    echo "ERROR: Neither SOURCE_DB_URL nor PGHOST is defined." >&2
    echo "Usage: SOURCE_DB_URL=\"postgresql://...\" ./scripts/db/backup.sh [output_path]" >&2
    exit 1
fi

echo "==> Starting database export to: ${OUTPUT_FILE}"
if [[ -n "${SOURCE_DB_URL:-}" ]]; then
    pg_dump \
        --dbname="${SOURCE_DB_URL}" \
        --format=custom \
        --no-owner \
        --no-acl \
        --file="${OUTPUT_FILE}"
else
    pg_dump \
        --format=custom \
        --no-owner \
        --no-acl \
        --file="${OUTPUT_FILE}"
fi

FILE_SIZE=$(du -h "${OUTPUT_FILE}" | cut -f1)
echo "==> Backup complete: ${OUTPUT_FILE} (${FILE_SIZE})"
echo "IMPORTANT: Encrypt this backup before storing offsite (e.g. gpg --symmetric ${OUTPUT_FILE})."
