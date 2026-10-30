# Backup and Recovery — Rorfost School Platform

## Multi-Tenant Data Safety & Migration Strategy

### The Golden Rule
**Prepare and test multi-tenancy while School 1 remains active on Aiven and current production code.** Never copy production database dumps or real student data into source control or AI tool sessions. Encrypted backups and restorations are executed solely by human operators on secure machines.

### Aiven → Neon Migration Rehearsal Plan (Sprints 22–23, 28)
1. **Operator Snapshot:** Export source PostgreSQL using `pg_dump` custom format (`-Fc --no-owner --no-acl`) to an off-repository encrypted volume using `scripts/db/backup.sh` (or `backup.ps1`).
   ```bash
   SOURCE_DB_URL="postgresql://user:pass@aiven-host:port/db?sslmode=require" \
   ./scripts/db/backup.sh ./backups/aiven_prod_pre_cutover.dump
   ```
2. **Staging Dry Run:** Restore snapshot to an isolated Neon branch/staging database using `scripts/db/restore.sh` (or `restore.ps1`). Verify `pg_restore` outputs, constraints, sequences, and extensions.
   ```bash
   TARGET_DB_URL="postgresql://user:pass@ep-staging.us-east-2.aws.neon.tech/neondb?sslmode=require" \
   ./scripts/db/restore.sh ./backups/aiven_prod_pre_cutover.dump
   ```
3. **Flyway Migration & History Verification:** Boot the backend against the staging database or run Flyway migrate (`V1..V27`). Verify `flyway_schema_history` checksums match and all migrations succeed.
4. **Data Parity & Schema Integrity:** Run `scripts/db/verify_parity.sh` (or `verify_parity.ps1`) to check table row counts, multi-tenant foreign keys, and database timezone (`UTC`).
   ```bash
   ./scripts/db/verify_parity.sh "postgresql://user:pass@ep-staging.us-east-2.aws.neon.tech/neondb?sslmode=require"
   ```
5. **Connection & Hikari Readiness on Render Free:**
   - Default pool size: `DB_POOL_MAX_SIZE=3` (Render Free 512MB RAM optimization).
   - Minimum idle connections: `DB_POOL_MIN_IDLE=0` (enables serverless Neon compute sleep when idle).
   - SSL mode required: `sslmode=require`.
   - Timezone configured: `UTC`.
6. **ImageKit Manifest:** Database backups do not contain binary file bytes. Maintain a separate inventory of ImageKit file IDs and object paths.

## Script Inventory (`scripts/db/`)

- `backup.sh` / `backup.ps1`: Exports database in custom format (`-Fc --no-owner --no-acl`).
- `restore.sh` / `restore.ps1`: Restores custom format dump (`--clean --if-exists --no-owner --no-acl`) with safety checks against accidental production restore.
- `verify_parity.sh` / `verify_parity.ps1`: Validates timezone (`UTC`), Flyway migration status, domain table counts, and tenant foreign key integrity without leaking student PII.

## Rollback Procedures

### Scenario A: Pre-Cutover Failure (Before Neon Writes)
- Point School 1 Cloudflare Worker back to the original Render deployment commit and Aiven DB.
- Revert configuration settings.
- Retain Aiven database untouched.

### Scenario B: Post-Cutover Failure (After Neon Writes)
- Rolling back after writes have occurred on Neon is a **data reconciliation incident**.
- Do not blindly restore an old database snapshot, as it will destroy new results, notices, or configuration changes made post-cutover.
- Freeze writes, perform a differential export of new records, and follow the documented human-approved reconciliation procedure.

## Regular Operational Backups

- Perform automated and periodic manual pg_dumps of the shared database.
- Store encrypted backups in secondary off-site storage.
- Periodically test database restoration in a disposable staging branch to verify RTO/RPO expectations.
