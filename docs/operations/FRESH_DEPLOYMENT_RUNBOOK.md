# Fresh Production Deployment Runbook (Neon + Render + Cloudflare Pages)

> **Document Type:** Production Runbook & Environment Variable Ledger  
> **Target Branch:** `feat/multi-tenancy`  
> **Topology:** Cloudflare Pages (Frontend Assets & Pages Functions Edge Gateway: `dhadhana-school-pages`) + Render Web Service (Spring Boot 3.4.1 / Java 21) + Neon Serverless PostgreSQL (Empty New Cluster) + Cloudflare Turnstile (Bot/Abuse Protection) + ImageKit CDN.

---

## 1. Architectural Overview & Boundaries

```
[Browser Client]
       │
       ▼ (HTTPS Same-Origin)
[Cloudflare Pages + Pages Functions] 
       ├─ Static Assets / SPA (Vite output: dist, _redirects: /* -> /index.html 200)
       ├─ Fail-closed Gateway Token validation (fails HTTP 500 if unset)
       ├─ Client-IP Injection (CF-Connecting-IP -> X-Portal-Client-IP; strips incoming x-portal-* headers)
       ├─ Edge Cache (/api/v1/public/* public tenant caches)
       └─ Gateway Token Injection (X-Portal-Gateway-Token from Pages secret)
       │
       ▼ (HTTPS Origin Proxy)
[Render Web Service] (Spring Boot 3.4.1, Java 21)
       ├─ Gateway Credential Authentication Filter (validates SHA-256 token digest)
       ├─ Turnstile Server-Side Validation (POST challenges.cloudflare.com/turnstile/v0/siteverify)
       ├─ Tenant Context Holder (ThreadLocal school context derived from gateway)
       ├─ Sliding-Window Login Rate Limiter (tenant-scoped in-memory keys)
       ├─ AdminBootstrapService (initial administrator creation upon first boot)
       ├─ Spring Session JDBC (session persistence in DB)
       └─ Flyway Migrations (V1..V30 append-only migrations)
       │
       ▼ (TLS Required sslmode=require)
[Neon PostgreSQL] (Fresh Database Cluster)
```

---

## 2. Environment Variables & Secret Separation Ledger

Every variable below is verified against actual codebase usage (`backend/src/main/resources/application.yml`, `backend/src/main/java/**`, `frontend/wrangler.toml`, and `frontend/src/**`).

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
| `CORS_ALLOWED_ORIGINS` | Yes | `https://dhadhana-school-pages.pages.dev` | Comma-separated list of exact allowed origins with `https://`. Example: `https://example.pages.dev,https://www.example.org`. Only actual production hostnames should be configured initially. |
| `UPLOAD_MAX_FILE_SIZE` | No | `10MB` | Max upload size for workbooks and album images. |
| `UPLOAD_MAX_REQUEST_SIZE`| No | `12MB` | Max multipart request envelope size. |
| `LOGIN_MAX_ATTEMPTS` | No | `5` | Maximum failed login attempts before temporary lockout. |
| `LOGIN_ATTEMPT_WINDOW` | No | `10m` | Sliding window duration for login attempt tracking. |
| `TURNSTILE_ENABLED` | Yes | `true` | Activates server-side Turnstile verification for login and result lookups. |
| `TURNSTILE_EXPECTED_HOSTNAMES` | Yes | `dhadhana-school-pages.pages.dev` | Comma-separated list of exact allowed hostnames without `https://`. Example: `example.pages.dev,www.example.org`. Only actual production hostnames should be configured initially. |
| `IMAGEKIT_URL_ENDPOINT`| Yes | `https://ik.imagekit.io/dhadhana` | Public ImageKit media CDN endpoint URL. |

### B. Render Backend — Secret Variables (Private Environment Secrets)

| Variable Name | Required | Example Format | Purpose & Safety Rule |
|:---|:---:|:---|:---|
| `DB_URL` | Yes | `jdbc:postgresql://ep-...neon.tech/neondb?sslmode=require` | Direct JDBC connection URL from Neon dashboard. Never commit. |
| `DB_USERNAME` | Yes | `neondb_app` | Database runtime user (use dedicated least-privilege role). |
| `DB_PASSWORD` | Yes | `[SECURE_NEON_PASSWORD]` | Password for database user. Never log or expose. |
| `TURNSTILE_SECRET_KEY` | Yes | `0x4AAAAAA...` | Cloudflare Turnstile server secret key. Validated against Cloudflare siteverify endpoint. |
| `IMAGEKIT_PRIVATE_KEY`| Yes | `private_...` | Private API key for ImageKit server-side upload and deletion. Backend only. |

### C. Render Backend — Temporary Admin Bootstrap Variables

| Variable Name | Required During Bootstrap | Example Value | Purpose & Lifecycle |
|:---|:---:|:---|:---|
| `ADMIN_INITIAL_EMAIL` | Yes | `admin@dhadhana.school` | Principal login email. |
| `ADMIN_INITIAL_PASSWORD` | Yes | `[SECURE_COMPLEX_PASSWORD]` | Temporary password encoded with BCrypt. Requires password change upon first login. |
| `ADMIN_INITIAL_SCHOOL_SLUG` | Yes | `dhadhana` | Target school slug (must already exist in database). |

> **Bootstrap Lifecycle:** All three values must be configured together. The account is created only if it does not already exist. **Delete these three variables from Render immediately after first successful login.**

> **Note on Gateway Authentication:** Plaintext `PORTAL_GATEWAY_TOKEN` is held solely by Cloudflare Pages runtime secrets. The Render backend does **NOT** accept a plaintext token environment variable; it validates the SHA-256 digest of the received `X-Portal-Gateway-Token` against the registered hash in the `school_gateway_credentials` database table.

### D. Cloudflare Pages — Plain Variables (`wrangler.toml` or Pages Dashboard)

| Variable Name | Set In | Value / Example | Purpose |
|:---|:---:|:---|:---|
| `BACKEND_ORIGIN` | `wrangler.toml` | `https://school-platform-gxrt.onrender.com` | Upstream Render web service HTTPS origin URL. |
| `SCHOOL_SLUG` | `wrangler.toml` | `dhadhana` | Injected diagnostic header `X-Portal-School-Slug` and cache namespace prefix. |

### E. Cloudflare Pages — Secret Variables (Pages Secret Store)

| Secret Name | Set Via Command | Example Format | Purpose |
|:---|:---:|:---|:---|
| `PORTAL_GATEWAY_TOKEN` | `wrangler pages secret put PORTAL_GATEWAY_TOKEN` | `[64_HEX_OR_BASE64_CHARACTERS]` | Secret token matched against hashed credential in DB table `school_gateway_credentials`. Pages Function fails with HTTP 500 if unset. |

### F. Frontend Build-Time Public Variables (`.env.production` / Pages Build Settings)

| Variable Name | Secret? | Example Format | Purpose |
|:---|:---:|:---|:---|
| `VITE_TURNSTILE_SITE_KEY` | **Public** | `0x4AAAAAA...` | Cloudflare Turnstile public site key used to render the challenge widget. |
| `VITE_API_BASE_URL` | **Public** | `""` (Empty string) | Empty string preserves same-origin `/api/*` calls routed through Pages Functions. |
| `VITE_SCHOOL_NAME` | **Public** | `શ્રી ઢાંઢણા પ્રાથમિક શાળા` | School display name substituted into HTML title at build time. |
| `VITE_SCHOOL_LOGO` | **Public** | `/school-logo.jpeg` | School logo path substituted into HTML favicon at build time. |
| `VITE_ASSET_BASE_URL` | **Public** | `https://ik.imagekit.io/dhadhana` | Public CDN asset URL endpoint. |

---

## 3. Fresh Neon Database Initialization Procedure

1. **Create New Project in Neon Console:**
   - Log into [console.neon.tech](https://console.neon.tech).
   - Click **Create Project** -> Project name: `rorfost-school-platform`.
   - Region: Select region closest to Render service (e.g. `ap-south-1` or `eu-central-1`).
2. **Retrieve Connection Details:**
   - Copy direct JDBC URL (`sslmode=require`).
   - Create database user and password.

---

## 4. Cloudflare Turnstile Configuration Sequence

1. **Open Cloudflare Dashboard -> Turnstile:**
   - Click **Add Site**.
   - Site name: `Dhadhana School Portal`.
   - Domain: enter the exact frontend hostname (`dhadhana-school-pages.pages.dev`). Do not include `https://` or trailing slashes.
   - Widget Mode: **Managed** (interactive challenge when risk detected).
2. **Collect Keys:**
   - **Site Key (Public):** Place into Pages build environment variables:
     `VITE_TURNSTILE_SITE_KEY=0x4AAAAAA...`
   - **Secret Key (Private):** Add to Render Environment Variables:
     `TURNSTILE_SECRET_KEY=0x4AAAAAA...`
     `TURNSTILE_ENABLED=true`
     `TURNSTILE_EXPECTED_HOSTNAMES=dhadhana-school-pages.pages.dev`
3. **Re-deploy Frontend & Restart Backend Service.**

> **Public Result Lookup Policy:** Turnstile is strictly anti-bot protection against automated scrapers. Public exam results are intentionally accessible with seat number per school policy; Turnstile ensures rate/bot protection rather than student authentication.

---

## 5. Rate Limiting Policy Note

Cloudflare edge rate limiting is currently **disabled**. All requests pass through Pages Functions directly to Render.
- Backend login rate limiting protects `/api/v1/admin/auth/login` (sliding window of 5 failed attempts per 10 minutes).
- Public exam result lookups and general API routes are protected by Turnstile anti-bot verification, not backend rate limiting.

---

## 6. Build and Deployment Commands

### Backend (Render Web Service)
- **Environment:** Docker or Java / Maven
- **Build Command:** `mvn -B clean package -DskipTests`
- **Start Command:** `java $JAVA_TOOL_OPTIONS -jar target/school-portal-0.0.1-SNAPSHOT.jar`
- **Health Check Path:** `/actuator/health`

### Frontend (Cloudflare Pages)
```bash
cd frontend
npm ci
npm run build
npx wrangler pages deploy dist --project-name=dhadhana-school-pages
```

---

## 7. Post-Deployment Verification Checklist

- [ ] `/actuator/health` on Render backend returns `{"status":"UP"}` without exposing internal details.
- [ ] Direct curl to Render backend `/api/v1/public/school` without gateway token returns `403 Forbidden`.
- [ ] Request to Cloudflare Pages `https://dhadhana-school-pages.pages.dev/api/v1/public/school` returns `200 OK` with school JSON.
- [ ] Direct page refresh on `/about` returns `200 OK` with client-side SPA routing (`_redirects` verified).
- [ ] Principal login with Turnstile challenge authenticates successfully and sets `SESSION` cookie with `HttpOnly; Secure; SameSite=Lax`.
- [ ] Initial password change screen appears and successfully updates administrator password.
- [ ] Temporary admin bootstrap variables (`ADMIN_INITIAL_*`) removed from Render dashboard.
