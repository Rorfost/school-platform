# API Guidelines — Rorfost School Platform

Use versioned REST paths: `/api/v1/public` for unauthenticated resources and `/api/v1/admin` for authenticated principal operations.

## Multi-Tenant API Routing: As-Is vs. Target

### Current State (As-Is)
- Public routes accept direct browser/client calls and query the first active school in PostgreSQL.
- Admin routes require a valid `PRINCIPAL` session and CSRF token.

### Target State (Sprints 03–05, 11–12)
- **Gateway Bearer Scheme:** Every `/api/*` call must originate from a verified Cloudflare Worker carrying `X-Portal-Gateway-Token`. Direct calls to Render without this secret token fail closed with 401/403 (with minimal `/actuator/health` as the only unauthenticated exception).
- **Tenant Context Binding:** The gateway filter hashes the token (SHA-256), resolves the mapped active school, and binds an immutable `TenantContext` to the request thread.
- **Admin Session Binding:** Admin endpoints strictly verify that the authenticated principal's session belongs to the requesting tenant (`PrincipalSession.schoolId == TenantContext.schoolId`). Cross-school session hijacking returns 403.

## Implemented Admin Endpoints

| Method and Path | Access | Purpose |
|---|---|---|
| `GET /api/v1/admin/auth/csrf` | Public | Supplies the SPA CSRF token and header name. |
| `POST /api/v1/admin/auth/login` | Public + CSRF | Authenticates principal scoped to `(school_id, email)` and creates JDBC session. |
| `POST /api/v1/admin/auth/logout` | Public + CSRF | Invalidates session; safe to repeat. |
| `GET /api/v1/admin/auth/me` | `PRINCIPAL` | Returns email, role, and password change status. |
| `PUT /api/v1/admin/auth/password` | `PRINCIPAL` + CSRF | Changes password (12–72 chars) with current password verification. |
| `POST /api/v1/admin/school/logo` | `PRINCIPAL` + CSRF | Uploads JPEG/PNG/WebP school logo to ImageKit under school prefix. |
| `DELETE /api/v1/admin/school/logo` | `PRINCIPAL` + CSRF | Removes school logo and triggers ImageKit cleanup. |
| `GET /api/v1/admin/dashboard/summary` | `PRINCIPAL` | Returns operational stats scoped to active school. |
| `GET /api/v1/admin/audit-logs` | `PRINCIPAL` | Returns paginated audit logs for the school. |
| `GET /api/v1/admin/academic/students` | `PRINCIPAL` | Lists students scoped to standard and academic year. |
| `POST /api/v1/admin/academic/students` | `PRINCIPAL` + CSRF | Enrolls new student under standard and academic year. |
| `PUT /api/v1/admin/academic/students/{id}` | `PRINCIPAL` + CSRF | Updates student name, roll number, or PIN hash. |
| `GET /api/v1/admin/materials` | `PRINCIPAL` | Lists all school materials including drafts. |
| `GET /api/v1/admin/notices` | `PRINCIPAL` | Lists all school notices including drafts. |
| `GET /api/v1/admin/gallery/albums` | `PRINCIPAL` | Lists all gallery albums for the school. |
| `GET /api/v1/admin/gallery/albums/{id}/images` | `PRINCIPAL` | Lists images in album. |
| `POST /api/v1/admin/gallery/albums` | `PRINCIPAL` + CSRF | Creates draft gallery album. |
| `POST /api/v1/admin/gallery/albums/{id}/publish` | `PRINCIPAL` + CSRF | Publishes album and its images. |
| `POST /api/v1/admin/gallery/albums/{id}/unpublish` | `PRINCIPAL` + CSRF | Returns album to draft status. |
| `DELETE /api/v1/admin/gallery/albums/{id}` | `PRINCIPAL` + CSRF | Deletes album and underlying ImageKit objects. |
| `GET /api/v1/admin/downloads` | `PRINCIPAL` | Lists all school downloads. |
| `POST /api/v1/admin/exam-results/upload` | `PRINCIPAL` + CSRF | Imports multi-subject workbook for `ANNUAL` or `EKAM_KASOTI`. |
| `POST /api/v1/admin/site-metrics/visits/reset` | `PRINCIPAL` + CSRF | Resets school visit counter. |

## Target Planned Endpoints

- `GET /api/v1/public/features` — Returns feature flags enabled for the requesting tenant.
- Operator gateway token rotation and provisioning endpoints (Sprint 15).

## Response & Error Conventions

- Use DTOs and Jakarta Validation; never return JPA entities directly.
- Problem Detail-style JSON (`application/problem+json`) with stable `code` and `requestId`.
- Admin responses enforce `Cache-Control: no-store`.
- Sensitive responses must never reveal stack traces, SQL errors, or foreign school existence.
