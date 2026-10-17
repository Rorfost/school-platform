# Security and Privacy — Rorfost School Platform

## Multi-Tenant Security Architecture: As-Is vs. Target

### Current State (As-Is)
- Single school administrative domain.
- Admin login queries by email across the entire database (`findAllByEmail`).
- Public requests query the first active school in PostgreSQL without authentication tokens.

### Target State (Multi-Tenant Plan)
- **Zero Trust in Unauthenticated Ingress:** Direct requests to Render without a verified `X-Portal-Gateway-Token` are rejected (with `/actuator/health` as the only unauthenticated exception).
- **Tenant Context Isolation:** The gateway filter resolves the school from the token digest, creating an immutable request-scoped `TenantContext`.
- **Tenant-Scoped Principal Authentication:** Admin login queries `(school_id, email)` within the resolved tenant context. The same email address can exist at multiple schools without credential collisions.
- **Session-Tenant Binding:** Every request to `/api/v1/admin/**` enforces that `PrincipalSession.schoolId == TenantContext.schoolId` via `AdminSessionTenantBindingFilter`. Cross-tenant session replay attempts fail closed with `403 cross_tenant_access_denied` without silently reassigning sessions. Administrative mutations (POST, PUT, PATCH, DELETE) re-verify that the principal account remains active in the database (`403 account_inactive_or_revoked`).

## Principal Authentication & Session Management

The admin panel uses Spring Security with server-side Spring Session JDBC records stored in PostgreSQL. There is one role: `PRINCIPAL`.
- `POST /api/v1/admin/auth/login` accepts validated credentials over HTTPS and authenticates via BCrypt. Failed logins return generic `401 authentication_failed`.
- The session ID is rotated at login and password change.
- `POST /api/v1/admin/auth/logout` invalidates the JDBC session.
- Passwords must be 12–72 characters and cannot match the existing password.

## Cookie and CSRF Rules

- **Cookie Boundaries:** Session cookies (`JSESSIONID`) are host-only, HttpOnly, Secure, and SameSite `Lax`. Setting `Domain=.workers.dev` is strictly prohibited to prevent cross-origin cookie sharing among Cloudflare Workers.
- **CSRF Protection:** CSRF is enforced on all administrative mutations. The frontend obtains a token via `GET /api/v1/admin/auth/csrf` and sends it in the `X-XSRF-TOKEN` header. Requests lacking a valid token fail with `403 csrf_invalid`.
- **CORS:** Permits credentials only for explicit Cloudflare Worker origins in `CORS_ALLOWED_ORIGINS`. Wildcards (`*`) with credentials are never allowed.

## Headers, Error Sanitization, and Rate Limiting

- **Security Headers:** The API enforces HSTS, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `X-Frame-Options: DENY`, a restrictive Content-Security-Policy, and a restrictive Permissions-Policy.
- **Sensitive Responses:** Admin responses and individual result lookups enforce `Cache-Control: no-store`.
- **Error Sanitization:** All errors return Problem Detail-style JSON (`application/problem+json`) with stable `code` and `requestId`. Stack traces, SQL errors, and internal tenant IDs are never leaked.
- **Rate Limiting:** Login attempts are rate-limited via a rolling-window limiter keyed by client IP and tenant scope (`429 login_rate_limited`).

## Student Data Privacy & Exam Results

- **No Government Identifiers:** The platform strictly prohibits reading, logging, hashing, or persisting government identifiers (such as Aadhaar UID). Student identity begins with school-issued G.R. numbers and roll numbers.
- **Individual Lookup Resistance:** Public result lookups return only individual student marksheets and resist automated enumeration. Class-wide result lists are never exposed publicly.
- **PIN Security:** Where student PINs are utilized, they are stored strictly as 60-character BCrypt hashes. Plaintext PINs and reversible encryption are prohibited.
