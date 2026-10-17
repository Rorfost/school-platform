# Rorfost School Platform

A reusable, Gujarati-first school platform with a focused principal-admin panel and public portal for primary and secondary schools. It is intentionally structured as a secure modular monolith, not a heavyweight ERP.

- **Product:** Rorfost School Platform (reusable multi-tenant codebase)
- **Reference Deployment (As-Is):** Dhadhana Primary School Portal (https://portal.dhadhana-school.workers.dev)

## Platform Architecture: As-Is vs. Target

### Current State (As-Is)
- Single-school production deployment.
- Frontend: Single Cloudflare Worker serving the SPA and proxying `/api/*`.
- Backend: Java 21 / Spring Boot 4.x modular monolith on Render Free.
- Database: Aiven PostgreSQL with Spring Session JDBC.
- Media: ImageKit single namespace.
- Tenant context: Controllers resolve the first active school in the database (`findFirstByIsActiveTrueOrderByCreatedAtAsc`).

### Target State (Planned Multi-Tenant Rollout)
- Multi-school shared platform serving 3+ schools on unified infrastructure.
- Frontend: Distinct Cloudflare Workers per school (`school-a`, `school-b`, `school-c`), each injecting a cryptographic `PORTAL_GATEWAY_TOKEN` secret to proxy `/api/*` to the backend.
- Backend: Single shared Spring Boot instance on Render Free enforcing request-scoped `TenantContext` via a gateway authentication filter.
- Database: Shared Neon PostgreSQL database with strict `school_id` isolation across all domain queries and mutations.
- Media: ImageKit with school-specific path namespaces (`schools/<school-uuid>/...`).
- Feature Flags: School-level feature enablement via database registry and UI gating.
- Detailed migration plan: [Multi-Tenant Rollout Runbook](docs/operations/TENANT_ROLLOUT.md).

## Features

### Public Portal
- Gujarati-first, mobile-friendly school information, principal profile, notices, photo gallery, downloads, and contact details.
- Student Corner: Published study materials, notices, downloads, and client-side School Tools.
- Examination Results: Individual student result lookup with privacy controls, Gujarati marksheet display, and single-page A4 print optimization.
- Controlled backend REST API serving only published content.

### Principal Admin Panel
- Secure principal session authentication, CSRF protection, password lifecycle, and audit logging.
- School settings and principal profile management.
- Academic configuration: Academic years, standards, subjects, and standard-subject mapping.
- Results & Marks: Assessment setup and marksheet management with draft/publish lifecycle states.
- Content Management: Study materials, notices, photo gallery with album-level publication and cover selection, and official downloads.

## Technology Stack

- **Frontend:** React 19, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod, Tailwind CSS, Lucide icons.
- **Backend:** Java 21, Spring Boot 4.x, Maven, Spring Security, Spring Session JDBC, Spring Data JPA, Flyway, PostgreSQL, ImageKit Java SDK, Actuator.
- **Infrastructure & Hosting:** Cloudflare Workers, Render Free, Neon PostgreSQL (target) / Aiven (as-is), ImageKit.

## Repository Structure

```text
frontend/       React/Vite SPA and Cloudflare Worker proxy
backend/        Spring Boot modular monolith
docs/           Product, architecture, engineering, data, planning, and operations docs
.github/        Continuous integration, Dependabot, and issue/PR templates
```

## Local Development

Prerequisites: Node 24+, Java 21, Maven 3.9+, Docker.

1. Start local PostgreSQL: `docker compose up -d`
2. Start frontend dev server: `cd frontend && npm install && npm run dev`
3. Start backend: `cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local`

Copy `.env.example` files to `.env.local` / local environment. Never put secrets in `VITE_*` variables as they are bundled into public browser assets.

See [Local Development Guide](docs/operations/LOCAL_DEVELOPMENT.md) for full commands and options.

## Documentation Index

- **Product:** [PRD](docs/product/PRD.md), [Requirements](docs/product/REQUIREMENTS.md), [Stakeholders](docs/product/STAKEHOLDERS.md), [Roadmap](docs/planning/ROADMAP.md)
- **Architecture:** [Architecture Overview](docs/architecture/ARCHITECTURE.md), [Flows](docs/architecture/FLOW.md), [Decisions (ADRs)](docs/architecture/DECISIONS.md)
- **Engineering:** [Code Rules](docs/engineering/CODE_RULES.md), [Security & Privacy](docs/engineering/SECURITY.md), [Config](docs/engineering/CONFIG.md), [Testing](docs/engineering/TESTING.md), [API Guidelines](docs/engineering/API_GUIDELINES.md)
- **Data:** [Database Architecture](docs/data/DATABASE.md), [Result Import Contract](docs/data/RESULT_IMPORT.md), [Exam Result Flow](docs/EXAM_RESULT_SYSTEM_FLOW.md)
- **Operations:** [Deployment Guide](docs/operations/DEPLOYMENT.md), [Tenant Rollout Checklist](docs/operations/TENANT_ROLLOUT.md), [Backup & Recovery](docs/operations/BACKUP_RECOVERY.md), [Local Development](docs/operations/LOCAL_DEVELOPMENT.md), [Operations Runbook](docs/operations/RUNBOOK.md)

## Security and Privacy

Do not commit credentials, real student records, examination PINs, result workbooks, database dumps, or confidential institutional documents. See [SECURITY.md](SECURITY.md) for vulnerability disclosure and safety protocols.

## Contributing

Read [AGENT.md](AGENT.md), [CODE_RULES.md](docs/engineering/CODE_RULES.md), and [CONTRIBUTING.md](CONTRIBUTING.md) before opening a change. Repository protection requires clean pull requests, green CI checks, and zero direct pushes to `main`.

## License & Copyright

This repository is publicly accessible for reference and inspection but is not open source. Copyright (c) 2026 Raj Patel and other identified contributors. All rights reserved. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
