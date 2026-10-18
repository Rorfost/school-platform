# Migration Baseline & Production Safety Inventory

This document establishes the verified operational baseline and safety freeze inventory for the multi-tenant migration of **Rorfost School Platform**.

## 1. Verified Release Baseline

| Property | Value / Status |
|---|---|
| **Production Source SHA** | `dafd00e7f32ae984f72fa51f8f0ceecf6d966d68` (main) |
| **Migration Branch** | `feat/multi-tenancy` |
| **Current Live Reference School** | Dhadhana Primary School (`dhadhana-school`) |
| **Live Frontend URL** | `https://portal.dhadhana-school.workers.dev` |
| **Render Backend Service** | `https://school-platform-gxrt.onrender.com` |
| **Current Production Database** | Aiven PostgreSQL (single-tenant instance) |
| **Target Production Database** | Neon PostgreSQL (`school-platform-prod` shared instance) |
| **Media Storage** | ImageKit (single account, moving to `schools/<uuid>/` prefixes) |
| **External Health Monitor** | UptimeRobot (configured for periodic health ping) |

## 2. Production Freeze Verification

- **Render Auto-Deploy:** Verified OFF in Render dashboard settings. Commits pushed to GitHub will not trigger an automatic backend deployment.
- **Cloudflare Worker Deployments:** Automatic production deployment from git is disabled / restricted to versions upload.
- **GitHub Actions Workflows:** Audited `.github/workflows/ci.yml`. Confirmed that GitHub Actions strictly runs automated validation (`npm run format:check`, `lint`, `typecheck`, `test`, `build`, and `mvn -B verify`) with zero automated deployment steps.

## 3. Environment Variable & Secret Inventory (Non-Secret Ledger)

| Variable Name | Component | Sensitivity | Role in Baseline |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Backend (Render) | Public | Set to `prod` |
| `DB_URL` | Backend (Render) | Sensitive | JDBC connection string (`sslmode=require`) |
| `DB_USERNAME` | Backend (Render) | Sensitive | Least-privilege PostgreSQL user |
| `DB_PASSWORD` | Backend (Render) | Secret | Database authentication credential |
| `DB_POOL_MAX_SIZE` | Backend (Render) | Config | Target: `3` (Free Render optimization) |
| `DB_POOL_MIN_IDLE` | Backend (Render) | Config | Target: `0` or `1` |
| `CORS_ALLOWED_ORIGINS` | Backend (Render) | Config | Allowed Worker origins |
| `FRONTEND_URL` | Backend (Render) | Config | Reference frontend URL |
| `SESSION_COOKIE_SECURE` | Backend (Render) | Config | `true` in production |
| `SESSION_TIMEOUT` | Backend (Render) | Config | `8h` |
| `LOGIN_MAX_ATTEMPTS` | Backend (Render) | Config | `5` |
| `LOGIN_ATTEMPT_WINDOW` | Backend (Render) | Config | `10m` |
| `UPLOAD_MAX_FILE_SIZE` | Backend (Render) | Config | `10MB` |
| `UPLOAD_MAX_REQUEST_SIZE` | Backend (Render) | Config | `12MB` |
| `APP_LOG_LEVEL` | Backend (Render) | Config | `INFO` |
| `IMAGEKIT_PRIVATE_KEY` | Backend (Render) | Secret | ImageKit upload/delete API key |
| `IMAGEKIT_URL_ENDPOINT` | Backend (Render) | Public | ImageKit public asset CDN endpoint |
| `PORTAL_GATEWAY_TOKEN` | Worker (Cloudflare) | Secret | Cryptographic routing bearer token (Sprint 04) |
| `BACKEND_ORIGIN` | Worker (Cloudflare) | Public | Render backend target origin |
| `VITE_API_BASE_URL` | Frontend (Build) | Public | Empty string (same-origin proxy) |
| `VITE_UPLOAD_BASE_URL` | Frontend (Build) | Public | Empty string (same-origin proxy) |
| `VITE_ASSET_BASE_URL` | Frontend (Build) | Public | Public ImageKit endpoint |

## 4. Risk Register

| Risk ID | Description | Severity | Mitigation Strategy |
|---|---|---|---|
| **RSK-001** | Free-tier resource limits (Render 750h/month, Neon compute hours, Cloudflare 100k requests/day). | High | Modest pool sizing (`DB_POOL_MAX_SIZE=3`), compute idle sleep support, no paid overages without human approval. |
| **RSK-002** | Cross-tenant data leakage if direct Render URL is called without tenant routing. | Critical | Enforce reverse-proxy gateway secret (`X-Portal-Gateway-Token`) in Spring Security filter; reject requests without valid token. |
| **RSK-003** | Rollback complexity after write operations begin on Neon. | High | Treat post-cutover rollback as a data reconciliation incident. Retain Aiven database until retention window passes. |
| **RSK-004** | Student marksheet privacy enumeration. | Medium | Enforce `Cache-Control: no-store` on all result lookups; validate standard and roll number strictly; audit public routes. |
| **RSK-005** | Real student records or PII leaking into repository or AI context. | Critical | Strict prohibition of real student data; automated test suites exclusively use synthetic fixtures with fictitious names. |

## 5. Review & Approval Checkpoints

1. **Sprint 02 Gate:** Human confirms freeze of School 1 auto-deploy on Render and Cloudflare; baseline tests pass.
2. **Sprint 05 Gate:** Reverse-proxy gateway token verification in place; forged client headers rejected.
3. **Sprint 14 Gate:** Principal isolation complete; Principal A cannot view or alter School B on any endpoint.
4. **Sprint 23 Gate (VERIFIED):** Dry run database restore to Neon staging branch rehearsed and verified with sanitized parity check, session truncation plan, zero orphan records, and documented runbook report in `MIGRATION_REHEARSAL_REPORT.md`.
5. **Sprint 27 Gate (VERIFIED):** Full staging acceptance checklist executed across School A, School B, and School C with zero cross-tenant defects; detailed acceptance report recorded in `STAGING_VERIFICATION_REPORT.md`.
6. **Sprint 28 Gate (VERIFIED):** Human-authorized production maintenance window, data freeze, encrypted backup, restore to Neon PostgreSQL, session truncation, row count parity ledger verified, and shared multi-tenant backend cutover completed; documented in `PRODUCTION_CUTOVER_REPORT.md`.
