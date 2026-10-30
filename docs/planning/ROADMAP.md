# Delivery Roadmap — Rorfost School Platform

## Original Product Delivery Phases (Baseline Status: COMPLETE)

- **Phase 0 — Foundation:** Java 21, Spring Boot 4.x, React 19, PostgreSQL Flyway schema, ImageKit abstraction, Docker setup. *(Complete)*
- **Phase 1 — Public School Website:** Home, About, Principal Desk, Contact, and responsive Gujarati layout. *(Complete)*
- **Phase 2 — Principal Authentication:** BCrypt credentials, Spring Session JDBC, CSRF protection, and password change lifecycle. *(Complete)*
- **Phase 3 — Academic Configuration:** Academic years, standards, subjects, and mappings. *(Complete)*
- **Phase 4 — Study Materials:** PDF upload, ImageKit storage, and Gujarati public viewer. *(Complete)*
- **Phase 5 — Generic Assessment System:** Assessment types, lifecycle states (`DRAFT`, `PUBLISHED`, `ARCHIVED`). *(Complete)*
- **Phase 6 & 7 — Examination Results & Public Lookup:** Annual and Ekam Kasoti workbook parsing, marks calculation, dynamic marksheet, and A4 print view. *(Complete)*
- **Phase 8 — Notices:** Notice management with attachments. *(Complete)*
- **Phase 9 — Gallery:** Photo albums with cover selection and order management. *(Complete)*
- **Phase 10 — Downloads:** Official document downloads. *(Complete)*
- **Phase 11 — Production Hardening:** Security headers, rate limiting, and error sanitization. *(Complete)*

---

## Multi-Tenant Migration Roadmap (30 Micro-Sprints)

This roadmap guides the controlled transition from single-school deployment to a shared multi-tenant platform.

| Stage | Sprints | Focus | Status |
|---|---|---|---|
| **Stage 1: Repository Structure & Baseline** | 01–02 | Package boundaries, generic platform docs, reproducible baseline tests | **Sprint 01 In Progress** |
| **Stage 2: Secure Tenant Routing** | 03–05 | Gateway credential schema, Cloudflare Worker header stripping & token injection, Render gateway filter | Planned |
| **Stage 3: Public Data Isolation** | 06–10 | Isolate school info, public content, academic lookup, exam results, and visit counters | Planned |
| **Stage 4: Principal Isolation** | 11–14 | Tenant-scoped login `(school_id, email)`, session binding, admin CRUD, and audit isolation | Planned |
| **Stage 5: Onboarding & Feature Flags** | 15–18 | School onboarding runbook, feature flag schema, backend enforcement, and React UI gating | Planned |
| **Stage 6: Frontend & Storage Isolation** | 19–21 | Dynamic school branding, independent Cloudflare Workers, ImageKit path namespaces | Planned |
| **Stage 7: Database Migration Planning** | 22–23 | Neon DB preparation, Aiven→Neon dry run, and data parity verification | Planned |
| **Stage 8: Assurance & Staging Validation** | 24–27 | Automated cross-tenant negative test suite, load validation, manual CI, staging 3-school acceptance | Planned |
| **Stage 9: Production Cutover** | 28–29 | Maintenance window, frozen data cutover to Neon, School 2/3 onboarding | Planned |
| **Stage 10: Production Security Release** | 30 | Dead code cleanup, environment audit, final security sign-off | Planned |

Each sprint is strictly sequential and human-gated.
