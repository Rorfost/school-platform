# API Guidelines

Use versioned REST paths: `/api/v1/public` for unauthenticated resources and `/api/v1/admin` for authenticated principal operations. Do not create a broad public data endpoint simply for frontend convenience.

## Implemented authentication endpoints

| Method and path | Access | Purpose |
| --- | --- | --- |
| `GET /api/v1/admin/auth/csrf` | Public | Supplies the SPA CSRF token and header name. |
| `POST /api/v1/admin/auth/login` | Public + CSRF | Authenticates an active principal and establishes a JDBC-backed session. |
| `POST /api/v1/admin/auth/logout` | Public + CSRF | Invalidates an existing session; repeated logout is safe. |
| `GET /api/v1/admin/auth/me` | `PRINCIPAL` | Returns email, role, and `mustChangePassword`; never credentials or session data. |
| `PUT /api/v1/admin/auth/password` | `PRINCIPAL` + CSRF | Verifies current password and sets a new 12–72 character password. |

Use DTOs and Jakarta Validation. Never return JPA entities. Authentication failures remain generic, and all frontend-visible text is represented by stable codes for Gujarati mapping.

- Use Problem Detail-style JSON with `code` and `requestId`; never disclose sensitive internals.
- Use 200/204 for success, 400 for validation or password-change rejection, 401 for absent/invalid authentication, 403 for forbidden/CSRF failure, 404 for an absent public resource, 409 for conflict, and 429 for rate limiting.
- Admin mutations require a valid CSRF token. The frontend obtains it from the CSRF endpoint and sends it in the returned header name.
- Credentialed CORS permits only configured origins. Authenticated endpoints never return wildcard origins.
- Paginated list endpoints define page size limits and response metadata. Filtering and sorting use explicit whitelists, never raw field names.
- Individual result responses will use `Cache-Control: no-store`; failed lookups must be generic enough to resist enumeration.
