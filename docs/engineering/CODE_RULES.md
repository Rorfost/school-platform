# Code Rules

## General

Prefer simple readable code; avoid over-engineering, dead code, speculative abstractions, and duplicated business logic. Keep methods and components small and cohesive, name things clearly, validate explicitly, and structure both applications by feature. Use comments only for why, constraints, non-obvious behavior, security choices, or important business rules; never narrate obvious code or add automated-assistance attribution.

## Backend

- Controllers are thin HTTP adapters: validate DTOs, delegate to services, and never return JPA entities.
- Services own business rules and clearly scoped transaction boundaries. Repositories focus on persistence queries.
- Use request/response DTOs, Jakarta Validation, consistent `ProblemDetail` errors, and explicit authorization.
- Do not expose internal exception details. Protect all admin paths with Spring Security and use Spring Session JDBC.
- Use `snake_case` database objects, UUIDs for externally exposed records, `TIMESTAMPTZ` timestamps, and Flyway for every schema change. Do not alter deployed schemas outside migrations.
- Test business rules, authorization, validation, persistence behavior, and relevant migration paths.

## Frontend

- Organize pages, components, hooks, API clients, and schemas by feature; avoid giant components.
- Use TanStack Query for server state and React Hook Form with Zod for forms. Do not add a global-state library without a demonstrated need.
- Handle loading, error, and empty states intentionally. Keep API details out of presentational components.
- Use semantic HTML, labels, keyboard access, visible focus, contrast, headings, alt text, and touch-friendly controls.
- Every visible string is Gujarati-first, simple, natural, and reviewed in context. Responsive design starts at mobile width.
