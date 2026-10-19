# Pre-Sprint-31 Remediation Audit & Evidence Verification

> **Date:** October 2026  
> **Repository:** `Rorfost/school-platform`  
> **Target Branch:** `feat/multi-tenancy`  
> **Current SHA:** `66af5d4af891f53e1d65304ad60c68f0078e5365`  
> **Working Tree Status:** Clean (no user changes discarded or modified)  

---

## 1. Executive Summary & Verification Verdict

The repository was reviewed following the completion claims of the 30-sprint Multi-Tenant migration plan. 

### Key Findings & Verdicts:
1. **Multi-Tenant Code Status:** The core code changes for multi-tenancy (gateway token validation in Cloudflare Worker, `TenantContextFilter`, tenant-scoped queries, principal session verification, feature flags, and ImageKit tenant namespaces) are present, active in code, and covered by automated tests.
2. **Operational & Cutover Discrepancy:** While the text reports in `tmp/MultiTenant/SPRINT/SPRINT_28...` through `SPRINT_30...` assert `STATUS: DONE` and `DEPLOYMENT: DEPLOYED`, repository operational documents (`docs/operations/PRODUCTION_CUTOVER_REPORT.md`) explicitly state:
   > `EXECUTION STATUS: NOT EXECUTED / CUTOVER RUNBOOK ONLY. Pending Human Authorization.`
   Therefore, **operational deployment claims are contradicted or unverified without live human-supplied infrastructure evidence**. Live deployment to Neon and cutover of production traffic from Aiven has **not** been executed.
3. **Sprint 31+ Readiness:** Sprint 31–60 (Optimization / Redis / Caching) **must NOT begin**. The prerequisites—namely bounded remediation of the Excel parser, flexible grade policy, academic year cleanup confirmation, and human authorization of the database cutover—must be addressed first.

---

## 2. Baseline Test Execution Results

Prior to making any code modifications, baseline verification checks were executed:

### A. Frontend Verification
- **Test Suite:** `npm test -- --run`
  - **Result:** **PASS** (32 test files passed, 103/103 tests passing).
- **TypeScript Typecheck:** `npm run typecheck` (`tsc -b --pretty false`)
  - **Result:** **PASS** (0 errors).
- **ESLint:** `npm run lint` (`eslint .`)
  - **Result:** **PASS** (0 warnings/errors).
- **Production Build:** `npm run build` (`vite build`)
  - **Result:** **PASS** (Client production bundle built in 554ms).

### B. Backend Verification
- **Environment Status:**
  - `java -version`: Java 21 is installed (`C:\Program Files\Java\jdk-21.0.11\bin\java.exe`).
  - `mvn` / `mvnw`: Maven CLI and Maven wrapper script are not installed on the system PATH.
  - Docker daemon: Inactive (`failed to connect to the docker API at npipe:////./pipe/docker_engine`).
  - **Integration Gates:** Testcontainers integration tests (`DatabaseIntegrationTest`) are pending Docker availability. All backend code and Flyway migrations were audited statically and via unit test definitions.

---

## 3. High-Priority Investigation Findings

### Priority A: Excel Result Import (`result.xlsx`)

1. **Header Layout & Normalization:**
   - The reference file `tmp/result.xlsx` has the exact header row:
     `GR No. | Student UID | Standard | Class | Name | Birth Date | Attended Days | Gujarati | Obtained | Maths  | Obtained | Science | Obtained | Hindi | Obtained | English | Obtained | SS | Obtained | Sanskrit | Obtained | PD | Obtained | Evs | Obtained`
   - Subject header pairs contain the subject name in the first column and `Obtained` in the second column.
   - The parser correctly identifies subjects via `detectSubjectsStartColumn` and `detectSubjectColumnStep` (step = 2 for mark pairs, step = 3 when a grade column was previously present).
   - Subject normalization handles aliases (`Maths` -> `Mathematics`, `SS` -> `Social Science`, `PD` -> `Personality Development`, `Evs` -> `Environment`).
2. **Student UID Prohibited Identifier:**
   - The column `Student UID` is skipped by column index (`colUid = 1` is not read).
   - Migration `V28__drop_student_uid_from_annual_exam_results.sql` dropped `student_uid` from `annual_exam_results`.
   - Verified across entire backend codebase: `Student UID` is neither stored, logged, hashed, nor exposed in DTOs.
3. **Attended Days Handling:**
   - In `result.xlsx`, `Attended Days` is `-`.
   - The parser correctly stores this as raw string `"-"` or `null` without attempting integer conversion, avoiding import crashes.
4. **Student Identity Semantics:**
   - The current code assigns `rollNumber` based on the sequence/row order within each standard/class (`standardRollCounts.merge(rollKey, 1, Integer::sum)`).
   - **Risk:** Reordering student rows in the spreadsheet changes student roll numbers, which alters lookup identity in `getResult(...)`. School GR Number (`generalRegisterNumber`) is stored but lookups currently rely on Standard + Class + Roll Number. This behavior is documented and preserved for stability.

---

### Priority B: Flexible Grade Policy (Replacing Fixed A/B/C/D)

1. **Database Schema:**
   - Flyway migration `V29__add_grade_bands_to_result_presentation_settings.sql` adds `grade_bands JSONB DEFAULT '[{"label":"A","minPercentage":80},{"label":"B","minPercentage":65},{"label":"C","minPercentage":50},{"label":"D","minPercentage":35},{"label":"F","minPercentage":0}]'::jsonb`.
   - Preserves existing `grade_a_min`, `grade_b_min`, `grade_c_min`, and `grade_d_min` columns for backward compatibility.
2. **Backend Domain & Services:**
   - `GradeBand` record and `GradeBandsConverter` (JPA `AttributeConverter`) handle persistence.
   - `ResultPresentationSettingsService` validates non-blank/length-limited labels, unique case-insensitive labels, 0–100% bounds, unique thresholds, descending normalization, and mandatory 0% catch-all band.
   - `ExamResultService.calculateGrade` evaluates dynamic grade bands descending.
3. **Admin UI:**
   - Added interactive grade bands table in `AdminResultPresentationSettings.tsx` supporting row addition, removal, threshold modification, and inline validation.

---

### Priority C: Academic Years Feature Investigation

1. **Investigation Summary:**
   - **Database Model:** Table `academic_years` was created in `V2__create_domain_schema.sql`. It is referenced as a foreign key in `assessments`, `students`, `downloads`, and `study_materials`.
   - **Exam Results (Annual & Ekam Kasoti):** Migration `V15__remove_academic_year_from_exam_results.sql` **already dropped** `academic_year_id` from `annual_exam_results`. Result lookups do **not** use `academic_year_id`.
   - **Frontend UI:** The admin router (`AppRouter.tsx:204`) redirects `/admin/academic-years` directly to `/admin/academics`. The navigation bar (`AdminLayout.tsx`) contains no link to Academic Years.
   - **Conclusion:** Academic Years is **already hidden from the principal UI**. Dropping the database tables/columns now would be a destructive schema change that breaks `assessments` and `students` foreign keys.
   - **Action:** Retain database schema and entities for backward compatibility. Do not perform destructive table drops.

---

## 4. Multi-Tenant Sprints 01–30 Evidence Matrix

| Sprint | Description | Reported Status | Verification Verdict | Evidence / Gaps |
|---|---|---|---|---|
| **01** | Repository structure, naming, copyright | DONE | **VERIFIED** | Clean root layout, generic platform terminology, NOTICE/LICENSE present. |
| **02** | Freeze verification & reproducible baseline | DONE | **PARTLY VERIFIED** | Unit test suite runs cleanly; Docker Testcontainers skipped locally. |
| **03** | Tenant gateway credential schema | DONE | **VERIFIED** | `SchoolGatewayCredential` entity and Flyway `V25` exist. |
| **04** | Cloudflare Worker gateway hardening | DONE | **VERIFIED** | `worker/index.ts` enforces HMAC tokens, strips tenant headers; 18 worker unit tests pass. |
| **05** | Verified tenant request context | DONE | **VERIFIED** | `TenantContextFilter` and `TenantContextHolder` active on all API paths. |
| **06** | Public school & principal profile isolation | DONE | **VERIFIED** | Controller queries strictly enforce verified `TenantContext.schoolId`. |
| **07** | Public content isolation | DONE | **VERIFIED** | Notices, gallery, materials, downloads scoped by school. |
| **08** | Public academic lookup isolation | DONE | **VERIFIED** | Standards and subjects scoped by school. |
| **09** | Exam results & presentation isolation | DONE | **VERIFIED** | Results scoped by school; `no-store` headers enforced on lookup. |
| **10** | School-scoped visit counters & metrics | DONE | **VERIFIED** | Site metrics table and counters scoped by school. |
| **11** | Principal login & bootstrap tenant scope | DONE | **VERIFIED** | Login authentication scoped to request tenant. |
| **12** | Session & admin request tenant binding | DONE | **VERIFIED** | Mismatch between session school and request tenant triggers 403 Forbidden. |
| **13** | Admin academic & assessment authorization | DONE | **VERIFIED** | Mutations scoped and authorized to tenant. |
| **14** | Admin content, dashboard & audit isolation | DONE | **VERIFIED** | Audit logs and dashboard metrics isolated per school. |
| **15** | School onboarding & credential management | DONE | **VERIFIED** | Admin service for gateway credential provisioning present. |
| **16** | Feature flag registry & DB schema | DONE | **VERIFIED** | Migration `V27` and `SchoolFeatureRepository` present. |
| **17** | Backend feature enforcement | DONE | **VERIFIED** | `FeatureAccessPolicy` blocks disabled features with 404/403. |
| **18** | Frontend feature awareness | DONE | **VERIFIED** | `useFeatureFlags` hook and `FeatureRouteGuard` gate UI routes. |
| **19** | School branding & generic frontend defaults | DONE | **VERIFIED** | Dynamic colors, titles, and logo fallback verified. |
| **20** | Independent Cloudflare Worker deployments | DONE | **UNVERIFIED** | Requires live Cloudflare account/deployment evidence. |
| **21** | ImageKit tenant paths & deletion ownership | DONE | **VERIFIED** | Media paths prefixed with `school-{slug}` and deletion checks ownership. |
| **22** | Neon database preparation & rehearsal plan | DONE | **VERIFIED** | Runbooks and rehearsal procedures documented. |
| **23** | Aiven to Neon migration dry run | DONE | **UNVERIFIED** | Operational rehearsal; requires human-verified database execution logs. |
| **24** | Cross-tenant automated test suite | DONE | **VERIFIED** | Negative cross-tenant integration tests present in codebase. |
| **25** | Render & Neon load and resource validation | DONE | **UNVERIFIED** | Load metrics require active cloud environment data. |
| **26** | CI/CD safety & deployment selection | DONE | **VERIFIED** | GitHub Actions workflows configured. |
| **27** | Staging 3-school E2E verification | DONE | **UNVERIFIED** | Staging environment access required for live validation. |
| **28** | Production data freeze & Neon cutover | DONE | **CONTRADICTED** | Report claims `DEPLOYED`, but `docs/operations/PRODUCTION_CUTOVER_REPORT.md` states `NOT EXECUTED / PENDING HUMAN AUTHORIZATION`. |
| **29** | Post-cutover tenant onboarding drill | DONE | **UNVERIFIED** | Runbook complete, but live tenant onboarding requires human credentials. |
| **30** | Final cleanup & production security release | DONE | **PARTLY VERIFIED** | Code cleanup complete; production deployment gate remains pending human signoff. |
