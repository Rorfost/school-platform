# Configuration

Use environment-based configuration only. Keep local, test, and production profiles separate. Commit example files, never actual `.env` files or secrets. Vite variables are compiled into public frontend assets and must never contain secrets.

## Backend environment

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local`, `test`, or `prod` profile selection. |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection. |
| `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE` | Optional conservative HikariCP limits; defaults are 5 and 1. |
| `CORS_ALLOWED_ORIGINS` | Explicit frontend origins allowed to call the API. |
| `SESSION_COOKIE_SECURE`, `SESSION_TIMEOUT` | Servlet session-cookie security and session lifetime; secure defaults to true outside local. |
| `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS` | Frontend origin and explicit browser origins permitted to call the API. |
| `UPLOAD_MAX_FILE_SIZE`, `UPLOAD_MAX_REQUEST_SIZE` | Conservative server request ceilings. They do not approve upload MIME types; a feature must define that policy before accepting files. |
| `ADMIN_INITIAL_EMAIL`, `ADMIN_INITIAL_PASSWORD`, `ADMIN_INITIAL_SCHOOL_SLUG` | First-principal bootstrap inputs. All three must be present only after the selected school exists. The password is BCrypt-hashed, never logged, and is never used to overwrite an existing administrator. Remove it after bootstrap. |
| `LOGIN_MAX_ATTEMPTS`, `LOGIN_ATTEMPT_WINDOW` | Optional local login limiter settings. Defaults are five failed attempts per direct client address in ten minutes. |
| `APP_LOG_LEVEL` | Application package logging level. Do not use logging configuration to expose request bodies, credentials, PINs, or storage secrets. |
| `R2_ENDPOINT`, `R2_REGION`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY` | S3-compatible endpoint, region, and credentials. |
| `R2_PUBLIC_BUCKET`, `R2_PRIVATE_BUCKET`, `R2_PUBLIC_BASE_URL` | Storage locations and public asset base URL. |

## Frontend environment

`VITE_API_BASE_URL` identifies the API base URL and `VITE_ASSET_BASE_URL` identifies public assets. Both are public configuration, not secret storage.

`backend/.env.example` and `frontend/.env.example` show every currently consumed environment variable and non-secret local addresses. The backend's `application.yml` defines shared defaults; `application-local.yml`, `application-test.yml`, and `application-prod.yml` isolate environment behavior. Production configuration, including database, R2, and bootstrap secrets, belongs only in the deployment provider's protected environment facility. Production must use HTTPS and keep `SESSION_COOKIE_SECURE=true`; only the local profile defaults this setting to false.
