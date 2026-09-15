# Security and Privacy

## Principal authentication

The V1 admin surface uses Spring Security with server-side Spring Session JDBC sessions in PostgreSQL. There is one role: `PRINCIPAL`. No JWT, Redis, or role hierarchy is used.

`POST /api/v1/admin/auth/login` accepts validated email and password over HTTPS. It returns the safe principal account view and creates a server-side session only after an active account and BCrypt password hash both match. Unknown email, inactive account, and incorrect password all return the same `401 authentication_failed` response. Passwords, hashes, cookies, session IDs, result PINs, and environment secrets are never logged or returned.

The application rotates a session ID during login and password change. `POST /api/v1/admin/auth/logout` invalidates any existing session and is safe to repeat. `GET /api/v1/admin/auth/me` and `PUT /api/v1/admin/auth/password` require a `PRINCIPAL` session. Password change verifies the current password, requires 12 to 72 characters, rejects reuse of the current password, clears `must_change_password`, rotates the session, and records an audit event.

## Initial principal bootstrap

Set `ADMIN_INITIAL_EMAIL`, `ADMIN_INITIAL_PASSWORD`, and `ADMIN_INITIAL_SCHOOL_SLUG` only for the first startup after the target school exists. If no `admin_users` record exists, the application creates one active `PRINCIPAL` account with a BCrypt hash and `must_change_password = true`. If any administrator already exists, it does nothing and never overwrites credentials. If configuration is incomplete or the named school is absent, it does nothing; no secret is written to logs.

Remove the initial password from the deployment environment after the first account has been created.

## CSRF, CORS, and cookies

Cookie authentication keeps CSRF enabled. The SPA first calls `GET /api/v1/admin/auth/csrf`, reads the returned token, and sends it in the named header (normally `X-XSRF-TOKEN`) for every state-changing API request. Spring also supplies the readable `XSRF-TOKEN` cookie needed by browser clients. Requests without a valid token receive `403 csrf_invalid`.

CORS permits credentials only for `CORS_ALLOWED_ORIGINS`; wildcard origins are not allowed. The local profile is the only profile that supplies a localhost origin and allows a non-Secure session cookie. Cookies are HttpOnly, SameSite `Lax`, and Secure by default. Production must run behind HTTPS with `SESSION_COOKIE_SECURE=true`.

## Headers, errors, logging, and rate limiting

The API applies HSTS on secure requests, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `X-Frame-Options: DENY`, an API-safe restrictive Content-Security-Policy, and a restrictive Permissions-Policy. Actuator exposes only health without details.

Errors use Problem Detail-style JSON with stable `code` and `requestId` fields. They never include stack traces, database details, class names, or submitted secrets. The frontend maps codes to Gujarati user text.

Every request logs request ID, method, route, status, and duration without recording request bodies. Authentication records only `LOGIN_SUCCESS`, `LOGIN_FAILED`, and `PASSWORD_CHANGED` in `audit_logs`. The local in-memory limiter permits a configurable number of failed login attempts per direct client address in a rolling window; it returns `429 login_rate_limited`. It is deliberately single-instance scope and will be revisited if deployment topology changes.

## Deferred controls

Email password reset, MFA, student accounts, result lookup, upload policy, and result-specific rate limiting are not implemented. Future work must extend the existing session, audit, privacy, and CSRF boundaries rather than bypassing them.
