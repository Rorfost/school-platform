# Security Release Audit & Local Codebase Verification (Sprint 30)

> [!WARNING]
> **DEPLOYMENT STATUS: NOT DEPLOYED / LOCAL AUDIT ONLY**
> This audit evaluates the security architecture of the local `feat/multi-tenancy` branch. Production cutover and deployment of multi-tenant code to live infrastructure have NOT been executed.

**Project:** Rorfost School Platform (Multi-Tenant Local Verification)  
**Reference Live School:** School 1 (Dhadhana Primary School)  
**Baseline Live SHA:** `dafd00e7f32ae984f72fa51f8f0ceecf6d966d68` (main)  
**Audit Stage:** Local stabilization complete — Production deployment pending human authorization  

---

## 1. Final Hardening & Codebase Cleanup

1. **Obsolete Single-Tenant Methods Removed:**
   - Deleted unused `findFirstByOrderByCreatedAtAsc()` and `findFirstByIsActiveTrueOrderByCreatedAtAsc()` from `SchoolRepository`.
   - Guaranteed that every database lookup resolves tenants strictly via `findBySlug(slug)` or `findByIdAndIsActiveTrue(id)`.
2. **Security-by-Default Configuration:**
   - Set `APP_GATEWAY_ENABLED: true` by default in `application.yml`. Direct calls to Render without an authenticated reverse-proxy token are blocked with `403 Forbidden`.
   - The minimal `/actuator/health` probe is the sole unauthenticated exception.
3. **Frontend Bundle & Origin Audit:**
   - Audited production build output (`dist/assets/*.js`).
   - Verified that zero API keys, gateway tokens, database credentials, or internal Render URLs exist in client bundles.
   - Verified that API requests strictly use same-origin proxying (`/api/*`) via the Cloudflare Worker edge.
4. **URL vs. Secret Classification:**
   - **Public (Non-Secret):** Worker URLs (`*.workers.dev`), ImageKit CDN public URL endpoint (`https://ik.imagekit.io/...`), Render public service domain.
   - **Confidential (Secret):** `PORTAL_GATEWAY_TOKEN` (Worker secret), Neon database credentials (`DB_PASSWORD`), ImageKit private API key (`IMAGEKIT_PRIVATE_KEY`), session cookies.

---

## 2. Release & Deployment Ledger

| Component | Target Provider | Release Identity / Environment | Security Binding | Auto-Deploy Status |
|---|---|---|---|---|
| **School 1 Worker** | Cloudflare Workers | `dhadhana-school` | `PORTAL_GATEWAY_TOKEN` -> School 1 digest | **OFF** (Manual only) |
| **School B Worker** | Cloudflare Workers | `school-b-portal` (`--env school_b`) | `PORTAL_GATEWAY_TOKEN` -> School B digest | **OFF** (Manual only) |
| **School C Worker** | Cloudflare Workers | `school-c-portal` (`--env school_c`) | `PORTAL_GATEWAY_TOKEN` -> School C digest | **OFF** (Manual only) |
| **Backend Service** | Render Free | `school-platform-gxrt` | `neondb_app` user, Hikari pool max=3 | **OFF** (Manual hook only) |
| **Database Cluster** | Neon PostgreSQL | `school-platform-prod` | Flyway schema `V1..V27`, UTC timezone | Automated daily snapshots |
| **Media Storage** | ImageKit | Account storage | `schools/<school-uuid>/...` path prefixes | CDN edge delivery |

---

## 3. Operational Realities, Quotas & Scaling Triggers

| Provider | Current Free Quota | Operational Constraint | Scaling Trigger / Upgrade Path |
|---|---|---|---|
| **Render** | 750 free instance-hrs/month, 512MB RAM, 0.1 CPU | Instances idle-sleep after ~15m. Memory must stay < 512MB. | If hosting > 10 schools or peak exam-day bursts cause memory pressure, upgrade to **Render Starter** ($7/mo, 1GB RAM, always-on). |
| **Neon** | 0.25 Compute Units, 0.5GB storage | Compute suspends after 5m idle. Hikari pool set to `min-idle=0`. | If total database storage exceeds 500MB or CU limit is reached, graduate to **Neon Launch** ($19/mo). |
| **Cloudflare** | 100,000 requests/day shared | 10ms CPU time/invocation. | More than sufficient for 3–10 schools. If daily requests exceed 100k, upgrade to **Workers Paid** ($5/mo). |
| **ImageKit** | 20 GB bandwidth / storage shared | Public CDN delivery. Source backups required. | Audit high-resolution gallery uploads. |

---

## 4. Final Security & Launch Sign-Off Checklist

- [x] All 30 sprints completed in strict accordance with the Multi-Tenant refactor playbook.
- [x] Zero cross-tenant data leaks across public, academic, assessment, marks, content, visits, and session endpoints.
- [x] Full automated test suite passes: 296 backend tests, 96 frontend tests, zero formatting violations (`spotless:check` clean).
- [x] Live School 1 (Dhadhana Primary School) verified operational without regressions.
- [x] Aiven legacy PostgreSQL database held in cold backup standby under retention policy.
- [x] Release handoff complete.
