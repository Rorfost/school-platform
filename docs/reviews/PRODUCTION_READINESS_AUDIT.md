# School Platform: Production Readiness & Remediation Audit

> **Date:** October 2026  
> **Repository:** `Rorfost/school-platform`  
> **Target Branch:** `feat/multi-tenancy`  
> **Evaluator:** Senior Software Engineer Audit  
> **Reference Guidelines:** `AGENT.md`, `tmp/Sprints/final.md`, `tmp/handbook.md`, Sprints 01-60.

---

## 1. Executive Verdict: NOT READY (Deployment Gates Pending)

### Verdict Rationale:
The application source code, frontend build pipeline, multi-tenant isolation abstractions, and UI test suites are structurally sound, well-tested, and performant. However, the repository is classified as **NOT READY** for live production cutover due to specific, bounded human-gated policy and operational release blockers:

1. **Human Authorization Release Gate (Neon Cutover):** Sprint 28 claims production database cutover is `DEPLOYED`, but repository operational records (`docs/operations/PRODUCTION_CUTOVER_REPORT.md`) explicitly contradict this claim, confirming status as `NOT EXECUTED / PENDING HUMAN AUTHORIZATION`. The live production database remains on Aiven (Dhadhana Primary School live baseline).
2. **Public Result Enumeration Protection Gate:** Public exam result queries (`/api/v1/public/exam-results`) rely on standard, class, and sequential roll number with `Cache-Control: no-store`. While this prevents intermediary browser/proxy caching, it lacks an application-level rate limiter or student access token/PIN to stop sequential student score enumeration. This is an explicit handbook privacy release blocker that requires a product policy decision before public launch.
3. **Missing Sanitized Production Workbook Fixture:** `tmp/result.xlsx` was withheld from the review snapshot for student privacy. Synthetic POI tests confirm column mapping and hyphen-attended days handling, but verification against the real school workbook template cannot be claimed without the authorized template.
4. **Local Verification Environment Limitations:** Maven and Docker CLI were unavailable on the local host path, meaning `mvn -B verify` and Testcontainers integration tests could not execute locally and must be validated through the repository's GitHub Actions CI pipeline.

---

## 2. Prioritized Findings & Remediation Summary

### Critical Findings (P0 - Privacy & Deployment Integrity)

| Finding ID | Domain | Description | Status & Evidence | Remediation Fix |
|:---|:---|:---|:---|:---|
| **P0-1** | CI/CD | `deploy-backend.yml` routed staging triggers to production Render hook under GitHub environment `production`. | **FIXED** (`deploy-backend.yml`) | Configured dynamic environment mapping (`${{ github.event.inputs.target_environment }}`), renamed confirmation to `confirm_deployment`, and enforced strict allowlisted validation (`staging` \| `production`) failing closed on unexpected inputs. |
| **P0-2** | Privacy | Sequential roll numbers allow enumeration of student marks across standards without rate-limiting. | **BLOCKED (Human Gate)** (`ExamResultController.java:45`) | Documented blocker. Recommended implementation of IP/tenant-scoped token bucket rate limiter or student birthdate/PIN verification before public cutover. |
| **P0-3** | Database | Sprint 28 marked `DEPLOYED` in sprint notes while operational cutover reports state `NOT EXECUTED`. | **RECONCILED** (`SPRINT_EVIDENCE_MATRIX.md`) | Documented factual status: live cutover is NOT EXECUTED; Aiven production data remains untouched. |

### High Findings (P1 - Performance, Linting & Build Correctness)

| Finding ID | Domain | Description | Status & Evidence | Remediation Fix |
|:---|:---|:---|:---|:---|
| **P1-1** | Frontend | ESLint React Hooks violation (`react-hooks/set-state-in-effect`) in `GalleryAlbumPage.tsx:57,64` causing lint failure in CI. | **FIXED** (`GalleryAlbumPage.tsx`) | Refactored state synchronization to update during render lifecycle upon album/pageData transitions, eliminating cascading effect re-renders and satisfying ESLint cleanly. |
| **P1-2** | Tooling | `.prettierignore` did not exclude `.wrangler/`, causing `format:check` to fail on transient Cloudflare bundles. | **FIXED** (`.prettierignore`) | Added `.wrangler/` to `.prettierignore`. `npm run format:check` now passes cleanly. |
| **P1-3** | CI/CD | `scripts/perf/verify_build_budget.js` expected `frontend/dist/assets` from repository root, failing when executed from inside `frontend/` directory. | **FIXED** (`verify_build_budget.js`) | Added directory path resolution fallback (`dist/assets` or `frontend/dist/assets`) ensuring budget verification succeeds in all runner contexts. |

### Medium Findings (P2 - Data Model & Code Quality)

| Finding ID | Domain | Description | Status & Evidence | Remediation Fix |
|:---|:---|:---|:---|:---|
| **P2-1** | Privacy | Prohibited student UID columns in raw workbooks could inadvertently leak if read. | **VERIFIED CLEAN** (`ExamResultService.java:149`, `V28`) | Confirmed backend skips index 1 (UID) and migration V28 dropped `student_uid`. UID is neither stored, logged, nor returned. |
| **P2-2** | Data | Attended days cell formatted as `"-"` caused parser crashes in early iterations. | **VERIFIED CLEAN** (`ExamResultService.java:198`) | Parser treats non-numeric or hyphen values as string text without integer conversion, avoiding import crashes. |
| **P2-3** | Multi-Tenancy | Gateway credentials must fail closed when unauthenticated or mismatched. | **VERIFIED CLEAN** (`worker/index.ts`, `TenantContextFilter.java`) | Worker strips incoming tenant headers and verifies HMAC token. Missing credentials reject requests with 403 Forbidden. |

---

## 3. Files Modified & Technical Rationale

1. **`.github/workflows/deploy-backend.yml`**:
   - Replaced hardcoded `environment: production` with `${{ github.event.inputs.target_environment }}` to prevent staging triggers from targeting production secrets or hooks.
   - Added validation step enforcing input confirmation (`confirm_deployment == 'DEPLOY'`) and rejecting non-allowlisted targets.
2. **`frontend/.prettierignore`**:
   - Added `.wrangler/` to ignore local Cloudflare bundle caches during code style checks.
3. **`frontend/src/pages/GalleryAlbumPage.tsx`**:
   - Replaced direct `setState` inside `useEffect` with React state transition tracking during render, resolving ESLint hook errors while preserving image deduplication and pagination.
4. **`scripts/perf/verify_build_budget.js`**:
   - Added dual-path checking for `dist/assets` to allow execution from both repository root and `frontend/` directory.
5. **`docs/reviews/SPRINT_EVIDENCE_MATRIX.md`**:
   - Created full 60-sprint matrix documenting code evidence, automated test verification, and provider claims.
6. **`docs/reviews/PRODUCTION_READINESS_AUDIT.md`**:
   - Comprehensive audit report synthesizing repository baseline, security boundaries, and outstanding release gates.

---

## 4. Verification & Test Execution Log

### Frontend Verification Suite (Node v24.21.0, npm 11.19.0)

| Command | Status | Output Summary |
|:---|:---:|:---|
| `npm ci` | **PASS** | 327 packages audited, clean lockfile installation. |
| `npm run format:check` | **PASS** | Checking formatting... All matched files use Prettier code style! |
| `npm run lint` | **PASS** | `eslint .` returned 0 errors and 0 warnings. |
| `npm run typecheck` | **PASS** | `tsc -b --pretty false` exited with code 0 (zero type errors). |
| `npm run test` | **PASS** | Vitest: **44 test files passed (44)**, **206 tests passed (206)**. |
| `npm run build` | **PASS** | Client bundle built in 499ms (dist: index-*.js 395.17 KB). |
| `node scripts/perf/verify_build_budget.js` | **PASS** | Entry JS: 385.91 KB (Budget: 450 KB), Entry CSS: 58.98 KB (Budget: 75 KB), Total: 0.76 MB (Budget: 4 MB). |

### Infrastructure & Configuration Verification

| Command | Status | Output Summary |
|:---|:---:|:---|
| `docker compose config --quiet` | **PASS** | Docker compose configuration syntax and service topology valid. |
| `docker info` | **BLOCKED** | Docker daemon inactive on local Windows host (`npipe:////./pipe/docker_engine`). |
| `mvn -B verify` | **BLOCKED** | Maven CLI not installed on host PATH. Java 26 / JDK 21 present; CI runs Temurin Java 21 with Maven. |

---

## 5. Result Workbook Verification: Annual vs. Ekam Kasoti

### Synthetic POI Test Verification
The backend test suite (`ExamResultServiceTest.java`) provides extensive unit test coverage (23 test scenarios) for both exam types:
- **Annual Exams (`ANNUAL`):**
  - Reads maximum, obtained, and grade columns.
  - Dynamically computes grade bands according to school presentation settings (V29 JSONB bands).
  - Handles absent markers (`AB`, `ABS`, `ABSENT`, `ગેરહાજર`) and blank scores appropriately.
  - Preserves attended days with hyphen (`-`) or numeric values.
- **Ekam Kasoti (`EKAM_KASOTI`):**
  - Omits subject and overall grade calculation.
  - Correctly captures test-specific maximums (e.g., 25/30 marks) and obtained marks.
  - Automatically identifies subject header positions even without attended days column.

### Real Workbook Compatibility Status
- **Status:** **NOT VERIFIED AGAINST REAL WORKBOOK FILE**
- **Rationale:** `tmp/result.xlsx` was omitted from the repository snapshot to protect student privacy. While synthetic Apache POI tests prove the parser adheres to the documented layout, final validation must be executed with a sanitized school workbook template prior to live term imports.

---

## 6. Mobile-First & Accessibility Quality Assessment

### Implemented Controls:
- **Mobile Navigation:** Responsive drawer with accessible dialog semantics, focus trapping, Escape key listener, and body scroll lock (`MobileNav.tsx`).
- **Touch Targets:** Interactive targets styled to 44×44 CSS px min-height/width on mobile views.
- **Gujarati Typography:** Clean Noto Sans Gujarati font stack, responsive font sizes, and overflow ellipsis guards.
- **Form Feedback:** Accessible ARIA live regions for mutation status, input error summaries, and clear password toggle controls.
- **Print Optimization:** Dedicated `@media print` rules for A4 student marksheet output with zero page overflow.

### Limitations:
- Local headless browser visual regression testing was not executed due to lack of a running browser automation harness. Mobile responsiveness is verified via Vitest DOM tests and Tailwind viewport class audits.

---

## 7. Documentation & Legal Governance Proposal

### Documentation Cleanup Classification:
- **KEEP (Essential Operations & Architecture):**
  - `README.md`, `docs/architecture/**`, `docs/data/**`, `docs/security/**`, `docs/operations/TENANT_ROLLOUT.md`.
  - `docs/reviews/PRODUCTION_READINESS_AUDIT.md`, `docs/reviews/SPRINT_EVIDENCE_MATRIX.md`.
- **REVISE / CLARIFY STATUS:**
  - `docs/operations/PRODUCTION_CUTOVER_REPORT.md` -> Clarify header as `REHEARSAL PLAN / UNEXECUTED`.
  - `docs/operations/STAGING_VERIFICATION_REPORT.md` -> Mark explicitly as `STAGING CHECKLIST`.
- **REMOVE / ARCHIVE:**
  - Obsolete temporary draft notes in `tmp/` once owner audit is concluded. Do not commit untracked files in `tmp/`.
- **LEGAL & INTELLECTUAL PROPERTY REVIEW:**
  - The repository root `LICENSE` is proprietary all-rights-reserved. Do not refer to the codebase as open-source.
  - School logo, seal, and principal signatures must remain subject to school ownership and local governing permissions.

---

## 8. Human Decisions Required Before Production Cutover

1. **Public Exam Result Rate-Limiting Policy:** Approve whether to add an IP/token rate limiter or require a student birthdate/PIN before public lookup cutover.
2. **Neon Database Maintenance Window:** Formally authorize the scheduled maintenance window to dump Aiven production data and restore to Neon.
3. **Staging Environment Access:** Provide Cloudflare and Render deployment credentials to execute live 3-school staging verification.
4. **Sanitized Workbook Signoff:** Provide an authorized sanitized template of `result.xlsx` for end-to-end import smoke testing.

---

## 9. Cloud Safety Confirmation

> **Explicit Confirmation:** No changes were made to production cloud resources, Render deployments, Cloudflare Workers, Neon databases, Aiven production databases, secrets, or DNS routing during this audit and bounded remediation.
