# Fresh Production Deployment Runbook (Neon + Render + Cloudflare)

> **Document Type:** Production Runbook & Environment Variable Ledger  
> **Target Branch:** `feat/multi-tenancy`  
> **Topology:** Cloudflare Worker (Frontend Assets & Edge Gateway) + Render Web Service (Spring Boot 3.4.1 / Java 21) + Neon Serverless PostgreSQL (Empty New Cluster) + Cloudflare Turnstile (Bot/Abuse Protection) + ImageKit CDN.

---

## 1. Architectural Overview & Boundaries

```
[Browser Client]
       │
       ▼ (HTTPS Same-Origin)
[Cloudflare Worker] 
       ├─ Static Assets / SPA (Vite output: /dist)
       ├─ Edge Rate Limiting (Workers Rate Limiting API: general API, login, results, uploads)
       ├─ Client-IP Injection (CF-Connecting-IP -> X-Portal-Client-IP; strips external X-Portal-* headers)
       ├─ Edge Cache (/api/v1/public/* public tenant caches)
       └─ HMAC Gateway Token Injection (X-Portal-Gateway-Token)
       │
       ▼ (HTTPS Origin Proxy)
[Render Web Service] (Spring Boot 3.4.1, Java 21)
       ├─ Gateway Token Verification Filter (fails closed without valid token)
       ├─ Turnstile Server-Side Validation (POST challenges.cloudflare.com/turnstile/v0/siteverify)
       ├─ Tenant Context Holder (ThreadLocal school context derived from gateway)
       ├─ Sliding-Window Login Rate Limiter (tenant-scoped in-memory keys)
       ├─ Spring Session JDBC (session persistence in DB)
       └─ Flyway Migrations (V1..V30 append-only migrations)
       │
       ▼ (TLS Required sslmode=require)
[Neon PostgreSQL] (Fresh Empty Database)
```

---

## 2. Environment Variables & Secret Separation Ledger

Every variable below is verified against actual codebase usage (`backend/src/main/resources/application.yml`, `backend/src/main/java/**`, `frontend/wrangler.jsonc`, and `frontend/src/**`).

### A. Render Backend — Plain Variables (Non-Secret)

| Variable Name | Required | Default / Example | Purpose / Verification in Code |
|:---|:---:|:---|:---|
| `SPRING_PROFILES_ACTIVE` | Yes | `prod` | Activates production Spring Boot profile (`application-prod.yml`). |
| `PORT` | Yes | `8080` | HTTP port dynamically injected by Render runtime (`server.port: ${PORT:8080}`). |
| `DB_POOL_MAX_SIZE` | No | `3` | Maximum HikariCP pool connections. Keep small (2–3) for Neon free tier. |
| `DB_POOL_MIN_IDLE` | No | `0` | Minimum idle connections (0 allows idle scale-to-zero on Neon). |
| `SESSION_TIMEOUT` | No | `8h` | Spring Session expiration window (`spring.session.timeout`). |
| `SESSION_COOKIE_SECURE`| No | `true` | Enforces `Secure` flag on `SESSION` cookies (`server.servlet.session.cookie.secure`). |
| `APP_LOG_LEVEL` | No | `INFO` | Root application package log level (`logging.level.com.rorfost.schoolportal`). |
| `APP_GATEWAY_ENABLED` | Yes | `true` | Rejects any request arriving without valid `X-Portal-Gateway-Token`. |
| `APP_PUBLIC_FEATURE_CACHE_ENABLED` | No | `true` | In-memory feature flag snapshot cache. |
| `CORS_ALLOWED_ORIGINS` | No | `https://dhadhana-school.workers.dev` | Explicit allowed origin (comma-separated if multiple). Avoid wildcards. |
| `UPLOAD_MAX_FILE_SIZE` | No | `10MB` | Max upload size for workbooks and album images. |
| `UPLOAD_MAX_REQUEST_SIZE`| No | `12MB` | Max multipart request envelope size. |
| `LOGIN_MAX_ATTEMPTS` | No | `5` | Maximum failed login attempts before temporary lockout. |
| `LOGIN_ATTEMPT_WINDOW` | No | `10m` | Sliding window duration for login attempt tracking. |
| `TURNSTILE_ENABLED` | Yes | `true` | Activates server-side Turnstile verification for login and result lookups. |
| `TURNSTILE_EXPECTED_HOSTNAMES` | Yes | `dhadhana-school.workers.dev` | Exact hostname(s) expected from Turnstile challenge response (comma-separated). |
| `IMAGEKIT_URL_ENDPOINT`| Yes | `https://ik.imagekit.io/dhadhana` | Public ImageKit media CDN endpoint URL. |

### B. Render Backend — Secret Variables (Private Environment Secrets)

| Variable Name | Required | Example Format | Purpose & Safety Rule |
|:---|:---:|:---|:---|
| `DB_URL` | Yes | `jdbc:postgresql://ep-...-pooler.c-2.neon.tech/neondb?sslmode=require` | Pooled JDBC connection URL from Neon dashboard. Never commit. |
| `DB_USERNAME` | Yes | `neondb_app` | Database runtime user (use dedicated least-privilege role). |
| `DB_PASSWORD` | Yes | `[SECURE_NEON_PASSWORD]` | Password for database user. Never log or expose. |
| `TURNSTILE_SECRET_KEY` | Yes | `0x4AAAAAA...` | Cloudflare Turnstile server secret key. Validated against Cloudflare siteverify endpoint. |
| `IMAGEKIT_PRIVATE_KEY`| Yes | `private_...` | Private API key for ImageKit server-side upload and deletion. Backend only. |

### C. Cloudflare Worker — Plain Variables (`wrangler.jsonc`)

| Variable Name | Set In | Value / Example | Purpose |
|:---|:---:|:---|:---|
| `BACKEND_ORIGIN` | `wrangler.jsonc` | `https://school-platform-gxrt.onrender.com` | Upstream Render web service HTTPS origin URL. |
| `SCHOOL_SLUG` | `wrangler.jsonc` | `dhadhana` | Injected diagnostic header `X-Portal-School-Slug` and cache namespace prefix. |

### D. Cloudflare Worker — Secret Variables (Wrangler Secret Store)

| Secret Name | Set Via Command | Example Format | Purpose |
|:---|:---:|:---|:---|
| `PORTAL_GATEWAY_TOKEN` | `wrangler secret put PORTAL_GATEWAY_TOKEN` | `[64_HEX_OR_BASE64_CHARACTERS]` | Secret token matched against hashed credential in DB table `school_gateway_credentials`. |

### E. Frontend Build-Time Public Variables (`.env.production`)

| Variable Name | Secret? | Example Format | Purpose |
|:---|:---:|:---|:---|
| `VITE_TURNSTILE_SITE_KEY` | **Public** | `0x4AAAAAA...` | Cloudflare Turnstile public site key used to render the challenge widget. |
| `VITE_API_BASE_URL` | **Public** | `""` (Empty string) | Empty string preserves same-origin `/api/*` calls routed through Worker. |

---

## 3. Fresh Neon Database Initialization Procedure

1. **Create New Project in Neon Console:**
   - Log into [console.neon.tech](https://console.neon.tech).
   - Click **Create Project** -> Project name: `rorfost-school-platform`.
   - Region: Select region closest to Render service (e.g. `ap-south-1` or `eu-central-1`).
   - Postgres version: `16` or `17`.
2. **Obtain Connection String:**
   - On the Neon Dashboard, select the **Pooled connection** checkbox.
   - Note the JDBC URL format:
     `jdbc:postgresql://[HOST]-pooler.neon.tech/[DATABASE]?sslmode=require`
3. **Least-Privilege Database Role (Recommended):**
   - Connect via `psql` to the direct connection string:
     ```sql
     CREATE ROLE neondb_app WITH LOGIN PASSWORD '[GENERATE_STRONG_PASSWORD]';
     GRANT CONNECT ON DATABASE neondb TO neondb_app;
     GRANT USAGE ON SCHEMA public TO neondb_app;
     GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO neondb_app;
     GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO neondb_app;
     ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO neondb_app;
     ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO neondb_app;
     ```
4. **Flyway Migration Execution on First Boot:**
   - The backend runs Flyway on startup (`spring.flyway.enabled=true`).
   - Because the database is completely fresh, Flyway will execute all 30 append-only migrations (`V1` through `V30`) sequentially.
   - Verification: `flyway_schema_history` table will be populated with 30 applied migrations.

---

## 4. First-School Provisioning & Gateway Token Registration

The backend requires that incoming `X-Portal-Gateway-Token` matches an active SHA-256 digest in `school_gateway_credentials`.

### Step 1: Run Operator Provisioning Command
In the Render shell (or a secure administrative CLI session with DB access):
```bash
java -Dspring.profiles.active=prod,operator-cli -jar target/school-portal-0.0.1-SNAPSHOT.jar \
  --provision.slug=dhadhana \
  --provision.name="Dhadhana Primary School" \
  --provision.hostname="dhadhana-school.workers.dev" \
  --provision.principalEmail="principal@dhadhana.edu" \
  --provision.initialPassword="[TEMPORARY_INITIAL_PASSWORD]"
```

**Output displayed once:**
- Generated `GATEWAY_TOKEN`: `[CRYPTOGRAPHIC_TOKEN_STRING]`
- School UUID and principal account registered in DB.

### Step 2: Inject Gateway Secret into Cloudflare Worker
```bash
cd frontend
npx wrangler secret put PORTAL_GATEWAY_TOKEN
# Paste the generated GATEWAY_TOKEN when prompted
```

---

## 5. Cloudflare Turnstile Configuration Sequence

1. **Open Cloudflare Dashboard -> Turnstile:**
   - Click **Add Site**.
   - Site name: `Dhadhana School Portal`.
   - Domain: enter the exact frontend hostname (e.g. `dhadhana-school.workers.dev` or custom domain `portal.dhadhana.org`).
   - Widget Mode: **Managed** (interactive challenge when risk detected).
2. **Collect Keys:**
   - **Site Key (Public):** Place into `frontend/.env.production`:
     `VITE_TURNSTILE_SITE_KEY=0x4AAAAAA...`
   - **Secret Key (Private):** Add to Render Environment Variables:
     `TURNSTILE_SECRET_KEY=0x4AAAAAA...`
     `TURNSTILE_ENABLED=true`
     `TURNSTILE_EXPECTED_HOSTNAMES=dhadhana-school.workers.dev`
3. **Re-deploy Frontend & Restart Backend Service.**

---

## 6. Build and Deployment Commands

### Backend (Render Web Service)
- **Environment:** Docker or Java / Maven
- **Build Command:** `mvn -B clean package -DskipTests`
- **Start Command:** `java $JAVA_TOOL_OPTIONS -jar target/school-portal-0.0.1-SNAPSHOT.jar`
- **Health Check Path:** `/actuator/health`

### Frontend (Cloudflare Worker)
```bash
cd frontend
npm ci
npm run build
npx wrangler deploy
```

---

## 7. Post-Deployment Verification Checklist

- [ ] `/actuator/health` on Render backend returns `{"status":"UP"}` without exposing internal details.
- [ ] Direct curl to Render backend `/api/v1/public/school` without gateway token returns `403 Forbidden`.
- [ ] Request to Cloudflare Worker `https://dhadhana-school.workers.dev/api/v1/public/school` returns `200 OK` with school JSON.
- [ ] Rapid requests (>10 requests in 60s) to `/api/v1/admin/auth/login` trigger `429 Too Many Requests`.
- [ ] Static assets (`/assets/*.js`, `/assets/*.css`) load with `200 OK` and are not affected by rate limits.
- [ ] Principal login with Turnstile challenge authenticates successfully and sets `SESSION` cookie with `HttpOnly; Secure; SameSite=Lax`.
