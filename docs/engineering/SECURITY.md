# Security and Privacy

## Implemented baseline

Spring Security denies every backend route except `/actuator/health` and future public API paths. CSRF remains enabled with a cookie-token repository so it is compatible with the future browser session flow. CORS accepts only configured origins, permits credentials, and does not use a wildcard. Servlet session cookies are HttpOnly, SameSite `Lax`, and Secure by default; local development explicitly opts out of Secure cookies. Spring Session JDBC tables are Flyway-owned, while no authentication flow creates sessions yet.

The Actuator web exposure contains health only, and health details are never public. Request IDs are returned in responses and attached to unexpected API errors without exposing exception internals. Credentials remain backend-only environment configuration; no secrets or private data belong in Git.

## Planned and deferred controls

Phase 2 will implement the `PRINCIPAL` login flow, BCrypt password hashing, session creation/invalidation, and protected admin authorization. Result lookup will resist enumeration: no class-wide results, hashed result PINs, generic invalid responses, `Cache-Control: no-store`, and a lightweight in-memory rate limit when needed. These controls are not implemented before the result requirements are approved.

Future upload features must validate authorization, size, declared type, detected content where feasible, and allowed MIME/extension policy. Store private files privately and use controlled access paths. Do not trust client filenames or content type. Define exact limits before upload features are released.

Future admin and bulk actions require audit logging. Apply least-data collection, protect backups, avoid sensitive logs, and restrict database/storage credentials to the backend throughout all phases.
