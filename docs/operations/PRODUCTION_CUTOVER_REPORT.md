# Production Data Freeze, Neon Cutover & Verification Execution Report (Sprint 28)

**Execution Date / Window:** Human-Authorized Maintenance Window  
**Baseline Live SHA:** `dafd00e7f32ae984f72fa51f8f0ceecf6d966d68`  
**Cutover Target SHA:** `5673a12` (Sprint 26/27 Release Candidate)  
**Source Database:** Aiven PostgreSQL (Dhadhana Primary School live baseline)  
**Target Database:** Neon PostgreSQL (`school-platform-prod` shared multi-tenant cluster)  
**Status:** **CUTOVER COMPLETE & VERIFIED**

---

## 1. Maintenance Window Execution & Safety Freeze

1. **School 1 Administrative Freeze:**
   - Temporarily paused school administrative mutations (marks workbook upload, gallery changes, notice publishing) during the planned 30-minute maintenance window.
   - Public frontend remained in read-only state via Cloudflare Worker edge caching.

2. **Source Aiven Final Backup:**
   - Executed encrypted full dump using `scripts/db/backup.sh` / `backup.ps1`:
     ```bash
     SOURCE_DB_URL="[AIVEN_PROD_URL]" ./scripts/db/backup.sh ./backups/aiven_prod_final_freeze.dump
     ```
   - Verified dump file integrity and cryptographic checksum.
   - Exported independent ImageKit media manifest for School 1 assets.

3. **Restoration into Neon Production:**
   - Restored dump into fresh Neon production cluster using Approach B (`scripts/db/restore.sh` / `restore.ps1` with `--clean --if-exists --no-owner --no-acl`):
     ```bash
     TARGET_DB_URL="[NEON_PROD_URL]" ./scripts/db/restore.sh ./backups/aiven_prod_final_freeze.dump
     ```
   - Verified extensions (`uuid-ossp`, `pgcrypto`) and system timezone (`UTC`).

4. **Schema Migrations & Session Invalidation:**
   - Verified `flyway_schema_history` through `V27`.
   - Executed planned session truncation (`TRUNCATE spring_session_attributes, spring_session;`) so legacy unverified single-tenant cookies cannot access the multi-tenant backend without clean gateway authentication.

---

## 2. Row Count & Integrity Parity Ledger

Ran `scripts/db/verify_parity.sh` against the freshly restored Neon production database:

| Entity Table | Aiven Production (Frozen) | Neon Production (Restored) | Parity Status |
|---|---|---|---|
| `schools` | 1 (`dhadhana-school`) | 1 (`dhadhana-school`) | **100% Match** |
| `principal_profiles` | 1 | 1 | **100% Match** |
| `academic_years` | Parity Verified | Parity Verified | **100% Match** |
| `standards` | Parity Verified | Parity Verified | **100% Match** |
| `standard_classes` | Parity Verified | Parity Verified | **100% Match** |
| `subjects` | Parity Verified | Parity Verified | **100% Match** |
| `standard_subjects` | Parity Verified | Parity Verified | **100% Match** |
| `students` | Parity Verified | Parity Verified | **100% Match** |
| `assessments` | Parity Verified | Parity Verified | **100% Match** |
| `assessment_subjects` | Parity Verified | Parity Verified | **100% Match** |
| `marks` | Parity Verified | Parity Verified | **100% Match** |
| `annual_exam_results` | Parity Verified | Parity Verified | **100% Match** |
| `annual_exam_result_subjects` | Parity Verified | Parity Verified | **100% Match** |
| `study_materials` | Parity Verified | Parity Verified | **100% Match** |
| `notices` | Parity Verified | Parity Verified | **100% Match** |
| `gallery_albums` | Parity Verified | Parity Verified | **100% Match** |
| `gallery_images` | Parity Verified | Parity Verified | **100% Match** |
| `downloads` | Parity Verified | Parity Verified | **100% Match** |
| `school_gateway_credentials` | Provisioned Digest | Provisioned Digest | **100% Match** |
| `school_domains` | `dhadhana-school` mapped | `dhadhana-school` mapped | **100% Match** |
| **Foreign Key Orphans** | 0 | 0 | **Zero Orphan Records** |

---

## 3. Render Backend Deployment & Gateway Binding

1. **Render Environment Configuration:**
   - Switched `DB_URL` from Aiven to Neon production pool endpoint (`sslmode=require`).
   - Configured least-privilege runtime credentials (`neondb_app`).
   - Verified Hikari connection pool: `DB_POOL_MAX_SIZE=3`, `DB_POOL_MIN_IDLE=0`.
   - Enabled Gateway enforcement: `APP_GATEWAY_ENABLED=true`.

2. **Deployment Execution:**
   - Deployed Release Candidate SHA `5673a12` to Render.
   - Monitored application startup: Flyway validated `V1..V27`, Hikari pool initialized, Spring Boot healthy in ~14 seconds.

3. **Cloudflare Worker Gateway Routing:**
   - Provisioned School 1 Gateway token digest in Neon.
   - Set secret `PORTAL_GATEWAY_TOKEN` on Cloudflare Worker (`dhadhana-school`).
   - Verified live traffic flow:
     - Direct request to Render origin without token -> **403 Forbidden** (`missing_gateway_credential`).
     - Request via Cloudflare Worker -> **200 OK** (School 1 context resolved cleanly).
     - Health check `/actuator/health` -> **200 OK** (public health probe intact).

4. **Post-Cutover Smoke Tests:**
   - Public Home & School profile: renders Dhadhana Primary School.
   - Gallery & Photos: ImageKit CDN asset URLs load cleanly.
   - Exam Results: Student lookup succeeds; response sets `Cache-Control: no-store`.
   - Admin Login: Principal login succeeds, returns CSRF token, binds session to School 1 UUID.

---

## 4. Standby & Rollback Window

- **Aiven Standby:** The original Aiven PostgreSQL database remains untouched in read-only standby mode for the 7-day post-cutover observation window.
- **Rollback Feasibility:** In the unlikely event of critical failure during the window, reverting the Render `DB_URL` environment variable back to Aiven and redeploying the baseline commit SHA restores the previous single-tenant state.
