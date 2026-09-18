# Rorfost School Portal

A reusable, Gujarati-first public school portal with a focused principal-admin panel for small schools. It is intentionally a secure modular monolith, not a full ERP.

**Live portal:** https://portal.dhadhana-school.workers.dev

## Current status

The public portal and principal-admin workflows for school settings, academic configuration, assessments, study materials, notices, gallery, downloads, account security, and JDBC-session authentication are implemented. Excel result import and public individual-result lookup remain blocked until the school supplies a privacy-safe, non-government student identifier and PIN workflow. Exam timetable and school timetable features are pending input or sample data.

## Features

### Public portal

- Gujarati-first, mobile-friendly school information, principal profile, notices, gallery, downloads, and contact details
- Student Corner for published study materials, notices, downloads, and client-side School Tools
- Published content is served through a controlled backend API

### Principal admin panel

- Secure principal session authentication, CSRF protection, password changes, and audit events
- School settings and principal profile management
- Academic years, standards, subjects, and standard-subject mapping
- Results & Marks setup with draft, publish, and archive states (using the internal generic assessment model)
- Study material, notice, gallery, and download management with ImageKit-backed uploads

## Architecture

React, TypeScript, and Vite provide the frontend. A Java 21 Spring Boot modular monolith exposes HTTPS REST APIs, uses PostgreSQL for relational data and JDBC-backed sessions, and stores public files in ImageKit. Cloudflare Workers serves the frontend and proxies API requests to the backend; the initial backend and database direction is Render and Aiven PostgreSQL.

## Technology stack

- Frontend: React, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod, Tailwind CSS, and Lucide
- Backend: Java 21, Spring Boot, Maven, Spring Security, Spring Session JDBC, Spring Data JPA, Flyway, PostgreSQL, ImageKit, and Actuator
- Deployment: Cloudflare Workers, Render, Aiven PostgreSQL, and ImageKit

## Repository structure

```text
frontend/       React/Vite SPA and Cloudflare Worker
backend/        Spring Boot modular monolith
docs/           Product, architecture, engineering, data, and operations documentation
.github/        Continuous integration, Dependabot, and contribution templates
```

## Local development

Install Node 24+, Java 21, Maven, and Docker. Start PostgreSQL with `docker compose up -d`, then run `npm run dev` in `frontend` and `mvn spring-boot:run -Dspring-boot.run.profiles=local` in `backend`.

Copy the component `.env.example` files only to ignored local environment files. Never put secrets in frontend `VITE_*` variables; those values are embedded in the browser bundle.

See [local development](docs/operations/LOCAL_DEVELOPMENT.md) for full setup and commands.

## Documentation

- [Product requirements](docs/product/PRD.md), [requirements](docs/product/REQUIREMENTS.md), and [roadmap](docs/planning/ROADMAP.md)
- [Architecture](docs/architecture/ARCHITECTURE.md), [flows](docs/architecture/FLOW.md), and [decisions](docs/architecture/DECISIONS.md)
- [Configuration](docs/engineering/CONFIG.md), [security](docs/engineering/SECURITY.md), and [testing](docs/engineering/TESTING.md)
- [Data model](docs/data/DATABASE.md) and [result-import constraints](docs/data/RESULT_IMPORT.md)
- [Deployment](docs/operations/DEPLOYMENT.md) and [runbook](docs/operations/RUNBOOK.md)

## Security and privacy

Do not commit credentials, real student data, result workbooks, database dumps, backups, or private school documents. See [SECURITY.md](SECURITY.md) for private reporting guidance.

## Contributing

Read [AGENT.md](AGENT.md), [CODE_RULES.md](docs/engineering/CODE_RULES.md), and [CONTRIBUTING.md](CONTRIBUTING.md) before opening a change. Recommended protection for `main` includes pull requests, required CI checks, resolved conversations, and no direct or force pushes.

## License

This repository is publicly viewable but is not open source. Copyright © 2026 Raj Patel / Rorfost. All rights reserved. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
