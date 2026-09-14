# Delivery Roadmap

Each phase is independently scoped. Do not start a phase until its dependencies and open decisions are resolved.

## Phase 0 — Foundation

**Goal:** create a runnable, documented baseline. **Scope:** repository, React/Vite and Spring Boot scaffolds, PostgreSQL/MinIO abstraction, Docker, Flyway, CI, baseline tests, and environment setup. **Dependencies:** this documentation. **Deliverables:** component build files, restricted health endpoint, Spring Session JDBC migration, CI workflows, local setup. **Tests:** frontend lint/typecheck/test/build; backend verify; infrastructure smoke test. **Definition of Done:** a new developer can start local infrastructure and both apps using documented commands. **Status:** initialized; keep Phase 0 checks healthy while beginning later phases.

## Phase 1 — Public school website

**Goal:** provide a Gujarati-first public school presence. **Scope:** school configuration, home, about, principal page, contact, responsive branding. **Dependencies:** Phase 0 and approved content/config fields. **Deliverables:** public pages and principal-managed configuration model. **Tests:** responsive, accessibility, Gujarati copy, and public API tests. **Definition of Done:** school information changes without source edits and works on a phone.

## Phase 2 — Principal authentication

**Goal:** protect administration. **Scope:** bootstrap, Spring Security, Spring Session JDBC, login/logout, password change, protected admin shell, CSRF. **Dependencies:** Phase 0 and approved bootstrap/credential handling. **Deliverables:** sole `PRINCIPAL` role and audited auth lifecycle. **Tests:** login/logout, protected routes, CSRF, cookie flags, password validation. **Definition of Done:** unauthenticated users cannot access admin operations and sessions behave correctly in production configuration.

## Phase 3 — Academic configuration

**Goal:** model school academic settings. **Scope:** academic years, standards, subjects, standard-subject mappings. **Dependencies:** Phases 0 and 2, confirmed school rules. **Deliverables:** admin CRUD and relational migrations. **Tests:** constraints, authorization, validation, archive/history behavior. **Definition of Done:** principal can safely configure an academic year without corrupting historical references.

## Phase 4 — Study materials

**Goal:** publish useful student resources. **Scope:** PDF upload, object storage, metadata, filters, publication state, admin CRUD, public Gujarati browsing. **Dependencies:** Phases 0–2 and upload policy. **Deliverables:** storage integration and public materials pages. **Tests:** access control, type/size rejection, metadata persistence, mobile browsing. **Definition of Done:** only published approved materials are available publicly.

## Phase 5 — Assessment system

**Goal:** configure generic assessments. **Scope:** assessment types, assessments, assessment subjects, future maximum/passing marks where applicable, draft/publish lifecycle. **Dependencies:** Phases 2–3 and approved academic policy. **Deliverables:** generic assessment domain and admin flow. **Tests:** state transitions, authorization, relational constraints. **Definition of Done:** an assessment can be created and remains non-public while draft.

## Phase 6 — Excel result import — BLOCKED UNTIL REAL EXCEL FORMAT IS PROVIDED

**Goal:** safely import workbook results. **Scope after approval:** analyze the actual workbook, document its contract, implement template/schema support, validation, preview, transactional import, duplicate handling, PIN behavior, and tests. **Dependencies:** Phases 2–5 and the owner-provided real workbook/template. **Deliverables:** documented workbook contract, parser, import audit/rollback behavior. **Tests:** representative owner-approved fixtures, invalid cases, preview/confirm, transaction rollback. **Definition of Done:** no schema assumptions remain undocumented and a valid workbook imports only after explicit confirmation.

## Phase 7 — Result lookup

**Goal:** let students see only their own published result. **Scope:** Gujarati lookup form, validation, approved PIN verification, result calculation/display, privacy, print style, no-store, rate limiting. **Dependencies:** Phase 6 and finalized public lookup rules. **Deliverables:** private individual public lookup. **Tests:** enumeration resistance, invalid generic response, no-store headers, published/draft behavior, mobile and print UI. **Definition of Done:** class-wide data cannot be accessed and draft results never appear publicly.

## Phase 8 — Notices

**Goal:** publish timely school notices. **Scope:** admin CRUD, optional approved attachments, publication state, public listings/details. **Dependencies:** Phases 1–2 and storage policy for attachments. **Deliverables:** notice management and Gujarati public UI. **Tests:** authorization, publication visibility, attachment access, empty/mobile states. **Definition of Done:** a principal can publish/unpublish notices safely.

## Phase 9 — Gallery

**Goal:** share school imagery responsibly. **Scope:** albums, image upload/order/captions, publication control, public gallery. **Dependencies:** Phases 1–2 and image/privacy policy. **Deliverables:** gallery metadata/storage and responsive gallery. **Tests:** image validation, ordering, authorization, alt text, mobile rendering. **Definition of Done:** only intended public images are accessible and accessible descriptions are supported.

## Phase 10 — Downloads and timetables

**Goal:** make official documents easy to find. **Scope:** downloadable documents and timetable files with metadata and publication state. **Dependencies:** Phases 1–2 and upload policy. **Deliverables:** admin management and public Gujarati resource pages. **Tests:** type/access validation, listing/filter behavior, phone downloads. **Definition of Done:** published documents are discoverable and private/unpublished files are not exposed.

## Phase 11 — Production hardening

**Goal:** launch safely and reliably. **Scope:** security headers, rate limiting, error handling, audit logs, indexes, backups, monitoring, E2E tests, deployment, domain/TLS, accessibility audit, Gujarati copy review, mobile UX review. **Dependencies:** completed intended product phases and production credentials/domains. **Deliverables:** deployment checklist, monitored release, recovery verification. **Tests:** security regression, production smoke, critical Playwright flow, restore exercise. **Definition of Done:** launch checklist passes, backup restoration is verified, and owner approves Gujarati/mobile experience.
