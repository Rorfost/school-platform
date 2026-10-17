# Configuration — Rorfost School Platform

Use environment-based configuration strictly. Keep local, test, and production profiles separated. Commit only example files (`.env.example`), never actual `.env` files or secrets. Vite variables (`VITE_*`) are compiled into browser assets and must never contain sensitive keys.

## Multi-Tenant Configuration: As-Is vs. Target

### Current State (As-Is)
- Single backend environment connecting to Aiven PostgreSQL.
- Single frontend origin configured via `FRONTEND_URL` and `CORS_ALLOWED_ORIGINS`.

### Target State (Multi-Tenant Plan)
- **Shared Backend (Render):** Connects to Neon PostgreSQL with SSL (`sslmode=require`). Free Render instances use modest pool sizing (`DB_POOL_MAX_SIZE=3`, `DB_POOL_MIN_IDLE=0` or `1`).
- **Cloudflare Workers:** Each school has a separate Worker environment with an offline-generated `PORTAL_GATEWAY_TOKEN` secret stored in Cloudflare. Render verifies token SHA-256 digests against the database, so Render never requires plaintext token lists in its environment.
- **ImageKit:** Public URL endpoint configured via `IMAGEKIT_URL_ENDPOINT`; private key restricted to Render secrets via `IMAGEKIT_PRIVATE_KEY`.

## Backend Environment Variables

| Variable | Purpose | Target Production Source | Notes |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active profile (`prod`, `local`, `test`) | Literal | Configures production security and logging |
| `DB_URL` | PostgreSQL JDBC connection string | Neon Connect dialog | Must use `jdbc:postgresql://...?...sslmode=require` |
| `DB_USERNAME` | Database username | Neon | Least privilege role |
| `DB_PASSWORD` | Database password | Neon | Secret; never in repo |
| `DB_POOL_MAX_SIZE` | HikariCP max connection pool | Provider config | Default `3` on Free Render to prevent exhaustion |
| `DB_POOL_MIN_IDLE` | HikariCP min idle connections | Provider config | Default `0` or `1` for serverless compute sleep |
| `CORS_ALLOWED_ORIGINS` | Permitted browser origins | Cloudflare Worker origins | Exact Worker URLs; no credentials wildcard |
| `FRONTEND_URL` | Reference frontend URL | Worker URL | Replaced by multi-origin registry in Sprint 20 |
| `SESSION_COOKIE_SECURE` | Secure flag on session cookies | Literal | `true` in production |
| `SESSION_TIMEOUT` | Principal session duration | Provider config | e.g. `8h` |
| `LOGIN_MAX_ATTEMPTS` | Maximum failed logins before lockout | Provider config | Default `5` |
| `LOGIN_ATTEMPT_WINDOW` | Lockout window duration | Provider config | Default `10m` |
| `UPLOAD_MAX_FILE_SIZE` | Maximum single upload size | Provider config | Default `10MB` |
| `UPLOAD_MAX_REQUEST_SIZE` | Maximum multipart request size | Provider config | Default `12MB` |
| `APP_LOG_LEVEL` | Application logging level | Provider config | Default `INFO`; no credentials or PII |
| `IMAGEKIT_PRIVATE_KEY` | ImageKit private API key | ImageKit dashboard | Secret; Render only |
| `IMAGEKIT_URL_ENDPOINT` | ImageKit public asset endpoint | ImageKit dashboard | Public CDN URL |
| `PORT` | HTTP server port | Render supplied | Default `8080` locally |

## Frontend Environment Variables (Compile-Time / Public)

| Variable | Purpose | Notes |
|---|---|---|
| `VITE_API_BASE_URL` | API base path | Empty string for same-origin Cloudflare proxy |
| `VITE_UPLOAD_BASE_URL` | Upload endpoint base path | Same-origin proxy |
| `VITE_ASSET_BASE_URL` | Public ImageKit base URL | Public CDN URL |

**Security Warning:** Never place database passwords, ImageKit private keys, portal gateway secrets, or admin credentials in `VITE_*` variables.
