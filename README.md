# Rorfost School Portal

A reusable, Gujarati-first school website and principal-admin portal for small schools (about 100–500 users). It is intentionally a simple, secure modular monolith—not a full ERP.

## Current status

Foundation only. The repository contains architecture, engineering rules, operational documentation, and empty component entry points. No product modules or domain-feature implementation exists yet.

## Architecture

React + TypeScript + Vite communicates over HTTPS REST with a Java 21 Spring Boot modular monolith. PostgreSQL stores relational data and session records; S3-compatible object storage stores files. Production direction is Cloudflare for frontend/DNS/R2, Render Free for the backend initially, and Aiven PostgreSQL.

## Stack

- Frontend: React, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod, Tailwind CSS, shadcn/ui where useful, Lucide
- Backend: Spring Boot 4.x, Maven, Spring MVC, Spring Security, Spring Session JDBC, Spring Data JPA, Flyway, PostgreSQL, Apache POI (planned), AWS SDK v2 S3 client, Actuator
- Local infrastructure: PostgreSQL and MinIO via Docker Compose

## Repository layout

```text
frontend/       Future React application
backend/        Future Spring Boot application
docs/           Product, architecture, engineering, data, operations, and roadmap docs
.github/        CI and dependency-management configuration
```

## Start here

1. Read [AGENT.md](AGENT.md) before modifying the repository.
2. Review [local development](docs/operations/LOCAL_DEVELOPMENT.md).
3. Use the [roadmap](docs/planning/ROADMAP.md) to choose the next scoped phase.

## Key principles

- All user-facing UI is simple, natural Gujarati; source code and documentation are English.
- Public experiences are mobile-first, accessible, calm, and school-friendly.
- The sole initial administrative role is `PRINCIPAL`; students, parents, and teachers do not have accounts.
- Result imports stay draft until explicit publication. Public individual results must never be cacheable.
- The Excel result format is pending. Do not implement or infer its schema before the owner supplies the real workbook/template.
- Avoid Redis, microservices, queues, GraphQL, and other unneeded infrastructure.

## Documentation

- [Product requirements](docs/product/PRD.md) and [stakeholders](docs/product/STAKEHOLDERS.md)
- [Architecture](docs/architecture/ARCHITECTURE.md), [flows](docs/architecture/FLOW.md), and [decisions](docs/architecture/DECISIONS.md)
- [Engineering rules](docs/engineering/CODE_RULES.md), [configuration](docs/engineering/CONFIG.md), [API guidelines](docs/engineering/API_GUIDELINES.md), [security](docs/engineering/SECURITY.md), and [testing](docs/engineering/TESTING.md)
- [Data model](docs/data/DATABASE.md) and [result-import constraints](docs/data/RESULT_IMPORT.md)
- [Operations](docs/operations/LOCAL_DEVELOPMENT.md) and [delivery roadmap](docs/planning/ROADMAP.md)

## Contribution workflow

Keep changes small and within the requested scope. Run relevant checks before each commit, update related documentation with behavioral changes, and use focused conventional commit messages. See [AGENT.md](AGENT.md) and [CODE_RULES.md](docs/engineering/CODE_RULES.md).

## License

Copyright © 2026 Rorfost. All rights reserved. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
