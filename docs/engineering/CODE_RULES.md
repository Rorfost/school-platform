# Code Rules — Rorfost School Platform

## General

Prefer simple, explicit, readable, and cohesive code. Avoid over-engineering, dead code, speculative abstractions, and duplicated business logic. Structure both applications by feature. Use comments only for why, constraints, non-obvious behavior, security choices, or important business rules; never narrate self-explanatory code or add automated-tool attribution.

## Multi-Tenant Isolation Rules

- **Zero Trust in Client Headers:** Never trust client-supplied `schoolId`, `slug`, `Host`, `Origin`, or custom headers for tenant identity.
- **Tenant Context Predicates:** All repository queries, joins, and entity updates must be explicitly scoped by `school_id` derived from the verified `TenantContext` or authenticated `PrincipalSession`.
- **Global Catalog Boundary:** Only explicitly designated global tables (e.g. `assessment_types`) may be queried without a `school_id` predicate.
- **Session-Tenant Binding:** Every administrative mutation must verify that the session's `schoolId` equals the request's `TenantContext.schoolId`.

## Backend

- Controllers are thin HTTP adapters: validate incoming DTOs, delegate to services, and never return JPA entities directly.
- Services own business transactions, validation, and authorization checks. Repositories focus strictly on persistence queries.
- Use request/response DTOs, Jakarta Validation, consistent Problem Detail errors (`application/problem+json`), and explicit HTTP status codes.
- Security: Protect admin paths with Spring Security, enforce CSRF, use Spring Session JDBC, and set `Cache-Control: no-store` on admin responses.
- Database: Use `snake_case` naming, UUIDs for external IDs, `TIMESTAMPTZ` timestamps, and append-only Flyway migrations. Never edit applied migrations.
- Style: Follow Java AOSP Spotless formatting (4-space indentation). Run `mvn spotless:apply` before opening PRs.

## Frontend

- Structure pages, components, hooks, and API clients by cohesive feature (`auth`, `public`, `school`, `tools`, `tenant`, `featureFlags`).
- Use TanStack Query for server state management and React Hook Form with Zod for form validation. Avoid unnecessary global state stores.
- Design: Mobile-first, responsive, accessible (proper ARIA roles, semantic HTML, visible focus states, high contrast).
- UI Copy: All user-facing text is natural, clear, simple Gujarati. English is reserved for developer code and admin configurations.
- Style: Adhere to Prettier and ESLint standards. Run `npm run format:check && npm run lint` before committing.
