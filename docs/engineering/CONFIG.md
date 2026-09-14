# Configuration

Use environment-based configuration only. Keep local, test, and production profiles separate. Commit example files, never actual `.env` files or secrets. Vite variables are compiled into public frontend assets and must never contain secrets.

## Backend environment

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local`, `test`, or `production` profile selection. |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection. |
| `CORS_ALLOWED_ORIGINS` | Explicit frontend origins allowed to call the API. |
| `SESSION_COOKIE_SECURE` | Secure-cookie setting; true in production. |
| `R2_ENDPOINT`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY` | S3-compatible endpoint and credentials. |
| `R2_PUBLIC_BUCKET`, `R2_PRIVATE_BUCKET`, `R2_PUBLIC_BASE_URL` | Storage locations and public asset base URL. |
| `ADMIN_INITIAL_EMAIL`, `ADMIN_INITIAL_PASSWORD` | One-time bootstrap inputs; handle safely and never log. |
| `FRONTEND_URL` | Canonical frontend URL. |

## Frontend environment

`VITE_API_BASE_URL` identifies the API base URL and `VITE_ASSET_BASE_URL` identifies public assets. Both are public configuration, not secret storage.

`backend/.env.example` and `frontend/.env.example` show local development values. Production configuration belongs in the deployment provider's protected secret/configuration facility.
