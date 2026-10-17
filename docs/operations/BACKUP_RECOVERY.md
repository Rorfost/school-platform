# Backup and Recovery — Rorfost School Platform

## Multi-Tenant Data Safety & Migration Strategy

### The Golden Rule
**Prepare and test multi-tenancy while School 1 remains active on Aiven and current production code.** Never copy production database dumps or real student data into source control or AI tool sessions. Encrypted backups and restorations are executed solely by human operators on secure machines.

### Aiven → Neon Migration Plan (Sprints 22–23, 28)
1. **Operator Snapshot:** Export Aiven PostgreSQL using `pg_dump` custom format (`-Fc --no-owner --no-acl`) to an off-repository encrypted volume.
2. **Staging Dry Run:** Restore snapshot to an isolated Neon branch/staging database. Verify `pg_restore` outputs, constraints, sequences, and extensions.
3. **Flyway Compatibility:** Verify `flyway_schema_history` checksums match existing V1–V24 migrations before applying any new multi-tenant migrations.
4. **Data Parity Verification:** Compare sanitized table row counts, UUID mappings, and metadata links (without exporting student PII into reports).
5. **ImageKit Manifest:** Database backups do not contain binary file bytes. Maintain a separate inventory of ImageKit file IDs and object paths.

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
