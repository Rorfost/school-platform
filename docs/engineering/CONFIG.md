# Configuration — Rorfost School Platform

Use environment-based configuration strictly. Keep local, test, and production profiles separated. Commit only example files (`.env.example`), never actual `.env` files or secrets. Vite variables (`VITE_*`) are compiled into browser assets and must never contain sensitive keys.

## Multi-Tenant Configuration: As-Is vs. Target

### Current State (As-Is)
- Single backend environment connecting to Aiven PostgreSQL.
- Single frontend origin configured via `FRONTEND_URL` and `CORS_ALLOWED_ORIGINS`.

### Target State (Production Architecture)
- **Shared Backend (Render):** Connects to Neon PostgreSQL with SSL (`sslmode=require`). Free Render instances use modest pool sizing (`DB_POOL_MAX_SIZE=3`, `DB_POOL_MIN_IDLE=0` or `1`).
- **Neon Connection Mode:** Connect using Neon's direct connection string for the backend service (Flyway migrations require transactional DDL locks that can conflict with external PgBouncer session mode, and HikariCP handles application-level pooling with max 3 connections).
- **Spring Session JDBC Sizing:** Configured with `flush-mode: on_save` and `save-mode: on_set_attribute` to eliminate redundant database writes on read-only authenticated HTTP requests.
- **Cloudflare Pages Frontend:** Production deployment hosted on Cloudflare Pages (`dhadhana-school-pages`), using Pages Functions (`functions/api/[[catchall]].ts`) for edge API proxying.
- **Gateway Authentication:** The Cloudflare Pages runtime holds the `PORTAL_GATEWAY_TOKEN` secret and injects it into upstream `/api/*` requests. Render verifies the token's SHA-256 digest against the database table `school_gateway_credentials`, ensuring Render never stores or requires plaintext token lists in its environment.
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
| `APP_GATEWAY_ENABLED` | Gateway authentication enforcement | Literal | `true` in production |
| `CORS_ALLOWED_ORIGINS` | Permitted browser origins | Cloudflare Pages origin | e.g. `https://dhadhana-school-pages.pages.dev` |
| `TURNSTILE_ENABLED` | Turnstile anti-bot verification | Literal | `true` in production |
| `TURNSTILE_SECRET_KEY` | Turnstile server secret key | Cloudflare Turnstile | Validated against Cloudflare siteverify |
| `TURNSTILE_EXPECTED_HOSTNAMES` | Allowed Turnstile hostnames | Literal | e.g. `dhadhana-school-pages.pages.dev` |
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
| `VITE_TURNSTILE_SITE_KEY` | Public Turnstile site key | Rendered in browser widget |
| `VITE_SCHOOL_NAME` | School display name | Substituted in index.html title |
| `VITE_SCHOOL_LOGO` | School logo asset URL | Substituted in index.html favicon |

**Security Warning:** Never place database passwords, ImageKit private keys, portal gateway secrets, or admin credentials in `VITE_*` variables.
