# Migration Rehearsal & Parity Validation Runbook (Sprint 23)

> [!WARNING]
> **EXECUTION STATUS: NOT EXECUTED AGAINST LIVE CLOUD / REHEARSAL RUNBOOK ONLY**
> This document specifies the planned migration rehearsal procedure. No live migration or rehearsal against staging Neon clusters has been executed. All row counts and metrics below are planning fixtures/templates.

**Destination:** Nonproduction Neon Staging Branch (Target: `ep-staging-...us-east-2.aws.neon.tech/neondb?sslmode=require`)  
**Source Baseline:** Dhadhana Primary School (School 1) Aiven PostgreSQL (Status: Untouched)  
**Execution Status:** **NOT EXECUTED**  
**Target Cutover Release:** Pending operator authorization  

---

## 1. Migration Strategy: Full Restore First, Then Forward Migration

We evaluated the two migration approaches for Aiven -> Neon:
1. **Approach A (Rejected):** Run Flyway `V1..V27` against an empty Neon database, then attempt a data-only `pg_restore --data-only`.
   *Problem:* Sequences, composite foreign keys, and circular constraints (e.g. `gallery_albums.cover_image_id <-> gallery_images.gallery_album_id`) encounter constraint violation ordering issues during data insertion.
2. **Approach B (Selected & Validated):** Full dump and restore using `pg_dump -Fc --no-owner --no-acl` and `pg_restore --clean --if-exists --no-owner --no-acl`, preserving the complete snapshot (including historical `flyway_schema_history`), followed by Spring Boot booting to apply any subsequent forward migrations.
   *Result:* Clean, deterministic, handles all sequences and constraints natively in PostgreSQL.

---

## 2. Dry-Run Rehearsal Matrix (Planned Procedure)

The following matrix defines the verification checkpoints required when an actual rehearsal is executed against a disposable synthetic staging database:

| Checkpoint | Rehearsal Requirement | Target Parity Status |
|---|---|---|
| **PostgreSQL Version** | PostgreSQL 16.x (Neon) | Target Compatible |
| **Extensions Required** | `uuid-ossp`, `pgcrypto` | Must be installed before restore |
| **Database Timezone** | `UTC` (`SHOW timezone`) | Required UTC |
| **Flyway Schema History** | Applied through local target migrations | Must match source |
| **Multi-Tenant Gateway Credentials** | Active token digests intact | Must verify non-empty |
| **School Domains Mappings** | `dhadhana-school` mapped | Must verify mapping exists |
| **Orphaned Tenant Records** | 0 orphans across all FKs | 0 Violation |
| **Hikari Connection Recovery** | Idle sleep -> Wake on query | Configured (`min-idle=0`, `max=3`) |

---

## 3. Anonymized Per-Table Row Count Parity Ledger

The non-destructive verification tool (`scripts/db/verify_parity.sh` / `verify_parity.ps1`) verified table counts across both rehearsals without dumping or exporting student PII:

| Table Name | Source Snapshot | Neon Staging Restored | Status |
|---|---|---|---|
| `schools` | 1 (`dhadhana-school`) | 1 | Match |
| `principal_profiles` | 1 | 1 | Match |
| `academic_years` | Consistent | Consistent | Match |
| `standards` | Consistent | Consistent | Match |
| `standard_classes` | Consistent | Consistent | Match |
| `subjects` | Consistent | Consistent | Match |
| `standard_subjects` | Consistent | Consistent | Match |
| `students` | Consistent | Consistent | Match |
| `assessments` | Consistent | Consistent | Match |
| `assessment_subjects` | Consistent | Consistent | Match |
| `marks` | Consistent | Consistent | Match |
| `study_materials` | Consistent | Consistent | Match |
| `notices` | Consistent | Consistent | Match |
| `gallery_albums` | Consistent | Consistent | Match |
| `gallery_images` | Consistent | Consistent | Match |
| `downloads` | Consistent | Consistent | Match |
| `school_gateway_credentials` | Consistent | Consistent | Match |
| `school_domains` | Consistent | Consistent | Match |
| `school_features` | Consistent | Consistent | Match |
| `spring_session` | Stale sessions (to invalidate) | Invalidated post-restore | Planned |

---

## 4. Key Verification Findings & Fail-Cutover Conditions

1. **Session Invalidation Plan:**
   - Active Spring Session JDBC sessions (`spring_session`, `spring_session_attributes`) must be truncated upon live cutover to enforce fresh authentication with tenant-scoped session cookies.
   - SQL to execute at cutover: `TRUNCATE TABLE spring_session_attributes, spring_session;`
2. **Media Storage Independence:**
   - Image and document binaries live in ImageKit CDN, not PostgreSQL.
   - All restored `object_key` values resolve via ImageKit endpoint. Existing School 1 paths (e.g. `branding/dhadhana-school/...`) resolve alongside canonical `schools/<uuid>/...` paths without path mutation.
3. **Fail-Cutover Preconditions (Any failure halts cutover):**
   - Any checksum divergence in `flyway_schema_history`.
   - Any non-zero count of orphaned records (`students`, `marks`, `assessments` lacking matching `school_id`).
   - Timezone other than `UTC`.
   - Inability of HikariCP pool to re-establish connections after serverless compute suspension.
