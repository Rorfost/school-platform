# Cloudflare Pages Deployment & Operations Guide

This runbook guides engineers through deploying the school portal frontend on **Cloudflare Pages**, backed by a **Spring Boot** application on **Render** and a **Neon PostgreSQL** database.

---

## 1. Target Architecture & Request Lifecycle

```
[ Browser ]
    │
    ├─ Static Assets / SPA HTML  ───> Cloudflare Pages Edge (Global CDN cache)
    │                                  (_redirects SPA fallback: /* -> /index.html 200)
    │
    └─ API Requests (/api/*)     ───> Cloudflare Pages Function (functions/api/[[catchall]].ts)
                                       ├── Path traversal inspection
                                       ├── Fail-closed Gateway Token validation (fails HTTP 500 if unset)
                                       ├── Edge Cache (Public GET allowlist)
                                       ├── Header Sanitisation (strips spoofed x-portal-*, x-forwarded-*)
                                       ├── Header Injection:
                                       │    ├── X-Portal-Gateway-Token (secret runtime binding)
                                       │    ├── X-Portal-Client-IP (from CF-Connecting-IP)
                                       │    └── X-Portal-School-Slug (env binding)
                                       └── Proxy to Backend Origin (Render)
                                                │
                                                ▼
                                    [ Render - Spring Boot ]
                                       ├── GatewayCredentialAuthenticationFilter (verifies X-Portal-Gateway-Token digest)
                                       ├── TenantFilter (resolves school by slug)
                                       ├── TurnstileVerificationService (siteverify check for login/results)
                                       └── Backend Rate Limiters (Login rate limiter)
                                                │
                                                ▼
                                    [ Neon Serverless PostgreSQL ]
```

---

## 2. Prerequisites & Accounts

1. **Cloudflare Account**: Standard Cloudflare account. Cloudflare edge rate limiting is currently disabled; all endpoint protection is handled by server-side rate limiters (e.g. login protection) and Cloudflare Turnstile bot prevention.
2. **Neon Account**: PostgreSQL instance provisioned with direct connection string for application-level HikariCP pooling.
3. **Render Account**: Web Service deployed running the backend Docker container or JAR.
4. **GitHub Repository**: Access to `school-platform` with permissions to configure Secrets and Actions.

---

## 3. Neon PostgreSQL Setup

1. Create a database project in Neon console: `school-portal-prod`.
2. Retrieve the direct JDBC connection string:
   ```text
   jdbc:postgresql://[endpoint].neon.tech/[dbname]?sslmode=require
   ```
3. Ensure SSL mode is set to `require`. Flyway migrations (`V1` to `V30`) execute automatically on backend boot.

---

## 4. Render Backend Service Configuration

In the Render Dashboard (Web Service `school-platform-api`):

### A. Non-Secret Environment Variables
| Variable Name | Value | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates production profile |
| `PORT` | `8080` | Server listen port |
| `APP_GATEWAY_ENABLED` | `true` | Enforces `X-Portal-Gateway-Token` header verification |
| `CORS_ALLOWED_ORIGINS` | `https://dhadhana-school-pages.pages.dev` | Comma-separated list of exact allowed frontend origins (with `https://`). Example: `https://example.pages.dev,https://www.example.org`. Only actual production hostnames should be configured initially. |
| `TURNSTILE_ENABLED` | `true` | Activates server-side Turnstile verification for admin login and public result lookups |
| `TURNSTILE_EXPECTED_HOSTNAMES` | `dhadhana-school-pages.pages.dev` | Comma-separated list of exact allowed hostnames without `https://`. Example: `example.pages.dev,www.example.org`. Only actual production hostnames should be configured initially. |
| `SESSION_COOKIE_SECURE` | `true` | Enforces `Secure` flag on `SESSION` cookies |
| `IMAGEKIT_URL_ENDPOINT` | `https://ik.imagekit.io/dhadhana` | Public CDN asset URL endpoint |

### B. Secret Environment Variables
| Variable Name | Example Format | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://ep-...neon.tech/neondb?sslmode=require` | Direct PostgreSQL JDBC connection URL |
| `DB_USERNAME` | `neondb_app` | Database username |
| `DB_PASSWORD` | `[SECURE_NEON_PASSWORD]` | Database password |
| `TURNSTILE_SECRET_KEY` | `0x4AAAAAA...` | Server-side Turnstile verification secret |
| `IMAGEKIT_PRIVATE_KEY` | `private_...` | Private key for server-side media upload |

> **Note on Gateway Authentication:** Plaintext `PORTAL_GATEWAY_TOKEN` is held solely by Cloudflare Pages runtime secrets. The Render backend does **NOT** accept a plaintext token environment variable; it validates the SHA-256 digest of the received `X-Portal-Gateway-Token` against the registered hash in the `school_gateway_credentials` database table.

---

## 5. Cloudflare Turnstile Configuration

1. In Cloudflare Dashboard, go to **Turnstile** -> **Add Site**.
2. **Site Name**: `School Portal Production`.
3. **Domain(s)**:
   - `dhadhana-school-pages.pages.dev` (Pages default production domain)
   - Do **NOT** include schemes (`https://`) or trailing paths (`/`).
4. **Widget Mode**: Managed.
5. Record the **Site Key** (public) and **Secret Key** (`TURNSTILE_SECRET_KEY` on Render backend).

> **Public Result Lookup Policy:** Cloudflare Turnstile serves as anti-bot / automated scraping defense for public exam result lookups and admin login. Per school product policy, public result lookup is intentionally open to allow parents and students to look up report cards by seat number; Turnstile prevents bulk extraction bots, but is not student-level authorization.

---

## 6. Cloudflare Pages Project Creation

### Method A: Git Integration (Recommended)
1. In Cloudflare Dashboard, navigate to **Workers & Pages** -> **Create application** -> **Pages** -> **Connect to Git**.
2. Select repository: `school-platform`.
3. Set build configuration:
   - **Framework Preset**: None / Vite
   - **Build command**: `npm run build`
   - **Build output directory**: `dist`
   - **Root directory**: `frontend`
   - **Production branch**: `feat/multi-tenancy` (or `main` when merged)
4. Configure Build-time Environment Variables in Pages Dashboard:
   - `VITE_SCHOOL_NAME`: e.g. `શ્રી ઢાંઢણા પ્રાથમિક શાળા`
   - `VITE_SCHOOL_LOGO`: e.g. `/school-logo.jpeg`
   - `VITE_TURNSTILE_SITE_KEY`: `<Turnstile Site Key>` (must match the Turnstile widget configured for `dhadhana-school-pages.pages.dev`)
   - `VITE_ASSET_BASE_URL`: `https://ik.imagekit.io/dhadhana`
   - `VITE_API_BASE_URL`: leave empty (empty string enables same-origin `/api/*` proxying)

### Method B: Wrangler / Direct Upload (Fallback & CI)
Deploy manually or via GitHub Actions using:
```bash
npx wrangler pages deploy dist --project-name=dhadhana-school-pages
```

---

## 7. Cloudflare Pages Functions & Secrets Configuration

Cloudflare Pages Functions are automatically served from `frontend/functions/`.

1. **Set Runtime Secrets**:
   Set `PORTAL_GATEWAY_TOKEN` via Wrangler CLI or Cloudflare Dashboard (**Pages Project -> Settings -> Environment Variables (Runtime)**):
   ```bash
   npx wrangler pages secret put PORTAL_GATEWAY_TOKEN --project-name=dhadhana-school-pages
   ```
   *If `PORTAL_GATEWAY_TOKEN` is unset or empty, the Pages Function fails closed with HTTP 500 (`gateway_configuration_error`) without proxying to Render.*

2. **Set Runtime Variables**:
   - `SCHOOL_SLUG`: `dhadhana`
   - `BACKEND_ORIGIN`: `https://school-platform-gxrt.onrender.com`

3. **Rate Limiting Status**:
   Cloudflare edge rate limiting is currently **disabled**. All requests pass through the Pages Function directly to Render. Backend login rate limiting remains active on `/api/v1/admin/auth/login`. Note that backend rate limiting only protects specific endpoints (such as admin login) and does not protect all API routes or public result lookups (which rely on Turnstile anti-bot verification).

---

## 8. Initial Principal / Administrator Setup (AdminBootstrapService)

The backend includes an automatic `AdminBootstrapService` to create the initial school administrator.

### Step-by-Step Bootstrap Procedure:

1. **Configure Temporary Render Environment Variables**:
   In the Render Web Service settings, add the following variables:
   | Variable Name | Example Value | Description |
   |---|---|---|
   | `ADMIN_INITIAL_EMAIL` | `admin@dhadhana.school` | Principal login email |
   | `ADMIN_INITIAL_PASSWORD` | `[SECURE_INITIAL_PASSWORD]` | Temporary complex password |
   | `ADMIN_INITIAL_SCHOOL_SLUG` | `dhadhana` | Target school slug (must already exist in database) |

2. **Start / Restart Backend**:
   - On startup, `AdminBootstrapService` verifies that all three variables are configured.
   - It checks that the school matching `ADMIN_INITIAL_SCHOOL_SLUG` exists.
   - It checks whether an admin user with `ADMIN_INITIAL_EMAIL` already exists for that school.
   - If not present, it encodes the initial password using **BCrypt** and saves the `AdminUser`.
   - The initial account requires an immediate password change upon login (`mustChangePassword: true`).

3. **Verify First Login**:
   - Log in at `https://dhadhana-school-pages.pages.dev/admin/login` using the bootstrap credentials and solve the Turnstile challenge.
   - You will be prompted to change the initial password. Update it immediately.

4. **CRITICAL: Remove Bootstrap Variables from Render**:
   - After the administrator account is created and verified, **delete** `ADMIN_INITIAL_EMAIL`, `ADMIN_INITIAL_PASSWORD`, and `ADMIN_INITIAL_SCHOOL_SLUG` from the Render environment variables.
   - This ensures sensitive initial credentials do not linger in server configuration.

---

## 9. Verification & Smoke Tests

Verify each core flow in order:

| Test Case | Method | Expected Output |
|---|---|---|
| Direct Route Refresh | Browse directly to `/about` or `/notices` and reload | HTTP 200, page loads (SPA `_redirects` verified) |
| Missing Gateway Token | Request `/api/v1/public/school` when `PORTAL_GATEWAY_TOKEN` is unset | HTTP 500 `gateway_configuration_error` |
| Public Exam Results | Search for seat number + solve Turnstile | Marksheet rendered; Turnstile verified server-side |
| Admin Login | Submit username/password + Turnstile | Auth cookie issued, redirect to `/admin/dashboard` |
| Gateway Header Stripping | Send `X-Portal-Client-IP: spoofed` to `/api/*` | Stripped and replaced with legitimate `CF-Connecting-IP` |
| Security Headers | Run `curl -I https://dhadhana-school-pages.pages.dev` | `X-Content-Type-Options`, `Content-Security-Policy` present |
| Admin Login Rate Limiting | Rapidly submit invalid credentials >5 times to `/api/v1/admin/auth/login` | HTTP 429 rate limit exceeded from backend |

---

## 10. Rollback Procedure

If a breaking defect is identified post-deploy:
1. In Cloudflare Dashboard, open **Workers & Pages** -> `dhadhana-school-pages` -> **Deployments**.
2. Identify the previous stable deployment.
3. Click the three dots `...` next to the deployment and click **Rollback to this deployment**.
4. Confirm rollback in the dialog. Routing reverts globally within seconds.
