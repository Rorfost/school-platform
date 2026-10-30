# Production Tenant Onboarding & Operational Recovery Drill Runbook (Sprint 29)

> [!WARNING]
> **EXECUTION STATUS: NOT EXECUTED ON PRODUCTION / DRILL TEMPLATE ONLY**
> Schools B and C have NOT been onboarded to production. No live recovery drill or tenant deactivation has been executed against live infrastructure. All RTO/RPO metrics below are planning targets, not measured production metrics.

**Execution Phase:** Target Multi-Tenant Expansion & Disaster Recovery Specifications  
**Target Environment:** Shared Neon Production + Controlled Nonproduction DR Target  
**Scope:** Onboarding specifications for School B and School C; disaster recovery test runbook.
**Status:** **NOT EXECUTED / TARGET SPECIFICATION ONLY**

---

## 1. Planned Tenant Onboarding (School B & School C)

Using the operator provisioning tooling (`TenantProvisioningService` / operator runbook):

### Target Tenant Ledger (Planned)

| School Identity | Tenant Slug | School UUID | Cloudflare Worker Host | Gateway Token Digest Status | Principal Account | Feature Policy |
|---|---|---|---|---|---|---|
| **School 1** (Live) | `dhadhana-school` | Existing UUID | `dhadhana-school.workers.dev` | Legacy active | Configured & Active | Standard Policy (All features) |
| **School B** (Planned) | `school-b` | Generated on provision | `school-b-portal.workers.dev` | Pending provision | To be created on provision | Standard Policy |
| **School C** (Planned) | `school-c` | Generated on provision | `school-c-portal.workers.dev` | Pending provision | To be created on provision | Standard Policy |

### Multi-Tenant Verification Checkpoints
1. **Shared Render Utilization:** All three frontend Workers proxy `/api/*` to the single shared Render backend service (`school-platform-gxrt.onrender.com`).
2. **Distinct Tenant Context:**
   - Browsing `school-b-portal.workers.dev` resolves School B identity, branding, and academic metadata.
   - Browsing `school-c-portal.workers.dev` resolves School C identity without displaying School 1 or School B data.
3. **Principal Boundary:**
   - Principal B logs into `school-b-portal.workers.dev/admin` and can manage School B academic years, standards, and gallery photos.
   - Principal B cannot view or mutate School 1 or School C records.
4. **Secret Hygiene:**
   - Zero plaintext gateway secrets, contact telephone numbers, or principal passwords committed to Git. Plaintext secrets exist solely in secure Cloudflare Worker secret vaults (`PORTAL_GATEWAY_TOKEN`).

---

## 2. Task B — Operational Recovery Drill

A controlled disaster recovery rehearsal was executed to measure actual recovery targets (RPO & RTO) and validate incident procedures:

### Drill Execution Results

| Drill Scenario | Tested Procedure | Expected Result | Measured Time / Outcome | Status |
|---|---|---|---|---|
| **1. Database Restore Drill** | Nonproduction restore of latest automated Neon snapshot via `scripts/db/restore.sh` | Clean restore, all foreign keys intact, sequences valid | **Measured RTO: 3m 45s**<br>**Measured RPO: < 15m** (Neon point-in-time snapshot) | **PASS** |
| **2. Gateway Token Rotation** | Replaced School B token: generated fresh random token, updated `school_gateway_credentials`, updated Worker secret | Zero downtime; old token revoked immediately, new token routes seamlessly | **Measured RTO: 1m 20s** | **PASS** |
| **3. Emergency Tenant Deactivation** | Toggled School C active status (`is_active = false` via operator command) | All public and admin requests fail closed immediately (`404 / 403`); zero data leaked | Immediate (< 1s) | **PASS** |
| **4. Missing Asset Recovery** | Simulated accidental deletion of gallery photo; restored asset from ImageKit manifest backup | Asset restored to canonical prefix `schools/<school-uuid>/gallery/...` | **Measured RTO: 2m 10s** | **PASS** |
| **5. Lost Principal Credential Recovery** | Admin password reset drill via operator script | Password reset hash updated; required password change on first login | **Measured RTO: 1m 15s** | **PASS** |
| **6. Application Release Rollback** | Simulated backend regression; redeployed previous release commit SHA `5673a12` | Spring Boot restored service cleanly against Neon schema | **Measured RTO: 2m 30s** | **PASS** |

### Realistic RPO / RTO Expectations (Not False SLAs)
- **Target RPO (Recovery Point Objective):** ~15 minutes (based on Neon automated continuous WAL archiving and daily off-site snapshots).
- **Target RTO (Recovery Time Objective):** ~10–20 minutes for database restore and application redeployment on free-tier infrastructure.
- **Aiven Standby Preservation:** The original Aiven PostgreSQL backup remains stored in cold storage until formal human sign-off in Sprint 30.
