# API Guidelines

Use versioned REST paths: `/api/v1/public` for unauthenticated resources and `/api/v1/admin` for authenticated principal operations. Do not create a broad public data endpoint simply for frontend convenience. These are conventions for future feature endpoints; no product API endpoints exist yet.

- Use DTOs and Jakarta Validation. Never return JPA entities.
- Use `ProblemDetail` for predictable errors, with a stable application error code and request ID where useful. Do not disclose sensitive internals.
- Use standard status codes: 200/201/204 success, 400 validation, 401 unauthenticated, 403 unauthorized, 404 absent public resource, 409 conflict, and 429 rate limited.
- Paginated list endpoints define page size limits and response metadata. Filtering and sorting use explicit whitelists, never raw field names.
- Admin mutation requests require an authenticated `PRINCIPAL` session and CSRF protection. CORS permits explicitly configured origins only.
- Individual result responses use `Cache-Control: no-store`; failed lookups must be generic enough to resist enumeration.
- Known future endpoint groups are public school/content/material/result lookup and admin school/academic/content/assessment/result workflows. Exact endpoints remain feature-time design work.
