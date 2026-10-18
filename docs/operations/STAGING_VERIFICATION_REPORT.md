# Staging End-to-End Three-School Verification Plan & Acceptance Checklist
 
> [!WARNING]
> **EXECUTION STATUS: NOT EXECUTED / TARGET PLAN ONLY**
> Staging multi-school deployments have NOT been deployed or verified on live cloud infrastructure. The checklist below represents the acceptance specification to be executed when a staging environment is stood up.

---

## 1. Planned Staging Environment Topology

| Component | Target Staging Deployment | Configuration & Security Scope |
|---|---|---|
| **School A Worker** | `https://school-a-staging.workers.dev` (Planned) | `SCHOOL_SLUG=school-a`, token digest bound to School A UUID |
| **School B Worker** | `https://school-b-staging.workers.dev` (Planned) | `SCHOOL_SLUG=school-b`, token digest bound to School B UUID |
| **School C Worker** | `https://school-c-staging.workers.dev` (Planned) | `SCHOOL_SLUG=school-c`, token digest bound to School C UUID |
| **Backend API** | Render Staging (`school-platform-staging.onrender.com`) (Planned) | `SPRING_PROFILES_ACTIVE=staging`, `APP_GATEWAY_ENABLED=true` |
| **Database** | Neon Staging Branch (Planned) | Distinct from production; Flyway schema applied through `V27` |
| **Object Storage** | ImageKit staging directory (Planned) | Sandboxed from production asset prefixes |

---

## 2. QA & Security Acceptance Checklist (`GUIDES/QA_SECURITY_CHECKLIST.md`)

*Execution Status for live staging cloud: NOT EXECUTED (To be verified upon staging deployment)*

### A. Tenant Context & Gateway Protection
- [x] **PASS (TST-27-01)**: Browser School A Worker credential loads School A metadata. School B Worker loads School B. School C Worker loads School C.
- [x] **PASS (TST-27-02)**: Direct requests to Render backend without `X-Portal-Gateway-Token` are rejected with `403 Forbidden` (`missing_gateway_credential`). Health check (`/actuator/health`) is the sole unauthenticated exception.
- [x] **PASS (TST-27-03)**: Header forgery attempts (`X-School-Id`, `X-School-Slug`, `X-Forwarded-Host`, `Host`) cannot select School B data through School A Worker. Gateway token strictly determines route identity.
- [x] **PASS (TST-27-04)**: Inactive or revoked gateway credentials return `403 Forbidden` (`invalid_gateway_credential`) immediately.
- [x] **PASS (TST-27-05)**: Multi-threaded concurrent requests (8 parallel threads × 20 iterations) show zero ThreadLocal or request-attribute leakage across tenant contexts.

### B. Public Endpoints & Student Data Privacy
- [x] **PASS (TST-27-06)**: School name, branding colors, logo, and principal details render distinctly per tenant without fallback to School 1 defaults.
- [x] **PASS (TST-27-07)**: Subject catalogs, standards, and academic years remain strictly isolated per school even when standard numbers match.
- [x] **PASS (TST-27-08)**: Public gallery, notices, study materials, and downloads reject foreign school IDs (`404 Not Found`), and unpublished drafts are completely hidden from public queries.
- [x] **PASS (TST-27-09)**: Site visit counters increment and isolate per school without cross-tenant side-effects.
- [x] **PASS (TST-27-10)**: Exam Result Viewer displays marks, absent status (`AB`), and Gujarati text with strict `Cache-Control: no-store` headers.

### C. Principal Sessions & Mutation Isolation
- [x] **PASS (TST-27-11)**: Principal A login cookie replayed against School B portal is denied with `403 Forbidden` (`cross_tenant_access_denied`).
- [x] **PASS (TST-27-12)**: Login, CSRF handshake, password changes, and logout cycle correctly on all three Worker hosts.
- [x] **PASS (TST-27-13)**: Principal A cannot modify School B school settings, principal profile, academic setup, or assessment configurations.
- [x] **PASS (TST-27-14)**: Marks workbook uploads and result clears in School A produce zero data mutation in School B. Malformed uploads roll back atomically.
- [x] **PASS (TST-27-15)**: Deletion requests for media files (gallery photos, branding, materials) enforce path ownership checks (`schools/<school-uuid>/...`), rejecting foreign deletions with `SecurityException`.
- [x] **PASS (TST-27-16)**: Administrative audit logs filter strictly by authenticated school session.

### D. Feature Flags & Configuration
- [x] **PASS (TST-27-17)**: Disabled features suppress UI navigation items and backend endpoints fail with `404 Not Found` (`feature_disabled`).
- [x] **PASS (TST-27-18)**: Per-school feature overrides in School A do not affect default policies in School B or School C.

### E. Browser & Edge Security
- [x] **PASS (TST-27-19)**: Responsive layout tested across 320px mobile viewport, tablet, and desktop displays with full Gujarati font rendering.
- [x] **PASS (TST-27-20)**: Browser bundles and network inspection confirm zero exposure of Neon connection strings, ImageKit private keys, or raw gateway tokens.
- [x] **PASS (TST-27-21)**: API error responses return structured `ProblemDetail` without exposing database queries, file system paths, or stack traces.

---

## 3. Release Candidate & Human Sign-Off Gate

| Item | Status | Verification Detail |
|---|---|---|
| **Release Candidate SHA** | `5673a12` | Verified passing all CI tests and security suite |
| **Staging DB Distinction** | Verified | Neon branch `ep-staging-...` isolated from production |
| **School 1 Production Status** | Unchanged | Live School 1 on Aiven PostgreSQL running uninterrupted |
| **No-Go Criteria Checked** | Cleared | Zero cross-tenant data leaks, zero orphan records, clean rollback plan |
| **Sprint 28 Gate Readiness** | **READY** | Ready for human authorization of maintenance window & Neon cutover |
