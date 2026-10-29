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
                                       ├── Edge Rate Limiting (Cloudflare RateLimit bindings)
                                       ├── Edge Cache (Public GET allowlist)
                                       ├── Header Sanitisation (strips spoofed x-portal-*, x-forwarded-*)
                                       ├── Header Injection:
                                       │    ├── X-Portal-Gateway-Token (secret binding)
                                       │    ├── X-Portal-Client-IP (from CF-Connecting-IP)
                                       │    └── X-Portal-School-Slug (env binding)
                                       └── Proxy to Backend Origin (Render)
                                                │
                                                ▼
                                    [ Render - Spring Boot ]
                                       ├── GatewayFilter (verifies X-Portal-Gateway-Token)
                                       ├── TenantFilter (resolves school by slug)
                                       ├── TurnstileService (siteverify check for login/results)
                                       └── Backend Rate Limiters
                                                │
                                                ▼
                                    [ Neon Serverless PostgreSQL ]
```

---

## 2. Prerequisites & Accounts

1. **Cloudflare Account**: Standard or Paid plan (Cloudflare Workers/Pages Rate Limiting bindings require a paid Workers plan).
2. **Neon Account**: PostgreSQL instance provisioned with pooled connection string.
3. **Render Account**: Web Service deployed running the backend Docker container or JAR.
4. **GitHub Repository**: Access to `school-platform` with permissions to configure Secrets and Actions.

---

## 3. Neon PostgreSQL Setup

1. Create a database project in Neon console: `school-portal-prod`.
2. Retrieve the connection string:
   ```text
   postgres://[user]:[password]@[endpoint]-pooler.neon.tech/[dbname]?sslmode=require
   ```
3. Ensure SSL mode is set to `require`. Flyway migrations (`V1` to `V30`) will execute automatically on backend boot.

---

## 4. Render Backend Service Configuration

In the Render Dashboard (Web Service `school-platform-api`):

1. **Environment Variables**:
   | Variable | Value | Notes |
   |---|---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` | Activates production profile |
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://[endpoint]-pooler.neon.tech/[dbname]?sslmode=require` | Connection pool URL |
   | `SPRING_DATASOURCE_USERNAME` | `[user]` | Neon user |
   | `SPRING_DATASOURCE_PASSWORD` | `[password]` | Neon password |
   | `PORTAL_GATEWAY_TOKEN` | `<64-char-random-secret>` | Shared gateway token |
   | `CF_TURNSTILE_SECRET_KEY` | `<turnstile-secret-key>` | Cloudflare Turnstile secret |
   | `CORS_ALLOWED_ORIGINS` | `https://<your-project>.pages.dev,https://school.example.com` | **Crucial:** Must include the Pages domain |

2. Deploy the service and verify the health check endpoint:
   ```bash
   curl -I https://<render-service-name>.onrender.com/actuator/health
   ```

---

## 5. Cloudflare Turnstile Configuration

1. In Cloudflare Dashboard, go to **Turnstile** -> **Add Site**.
2. **Site Name**: `School Portal Production`.
3. **Domain(s)**:
   - `<project-name>.pages.dev` (Pages default domain)
   - `school.example.com` (Custom production domain, if used)
   - Do **NOT** include paths or schemes (e.g., `https://` is invalid in Turnstile hostnames).
4. **Widget Mode**: Managed.
5. Record the **Site Key** (public) and **Secret Key** (backend only).

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
4. Configure Build-time Environment Variables:
   - `VITE_SCHOOL_NAME`: e.g. `શ્રી ઢાંઢણા પ્રાથમિક શાળા`
   - `VITE_SCHOOL_LOGO`: e.g. `/school-logo.jpeg`
   - `VITE_TURNSTILE_SITE_KEY`: `<Turnstile Site Key>`
   - `VITE_ASSET_BASE_URL`: `https://<render-service-name>.onrender.com`

### Method B: Wrangler / Direct Upload (Fallback & CI)
Deploy manually or via GitHub Actions using:
```bash
npx wrangler pages deploy dist --project-name=dhadhana-school-pages
```

---

## 7. Cloudflare Pages Functions & Secrets Configuration

Cloudflare Pages Functions are automatically picked up from `frontend/functions/`.

1. **Set Runtime Secrets**:
   Use Wrangler CLI or the Cloudflare Dashboard under **Pages Project -> Settings -> Environment Variables (Runtime)**:
   ```bash
   npx wrangler pages secret put PORTAL_GATEWAY_TOKEN --project-name=dhadhana-school-pages
   ```
2. **Set Runtime Variables**:
   - `SCHOOL_SLUG`: `dhadhana`
   - `BACKEND_ORIGIN`: `https://<render-service-name>.onrender.com`
3. **Rate Limiting Bindings (Paid Plan)**:
   In Cloudflare dashboard, go to **Workers & Pages** -> **Rate Limiting**, create namespaces and bind them under **Pages Project Settings -> Functions -> Rate Limit Bindings**:
   - `API_RATE_LIMITER` (Limit: 120, Period: 60s)
   - `LOGIN_RATE_LIMITER` (Limit: 10, Period: 60s)
   - `RESULT_RATE_LIMITER` (Limit: 10, Period: 60s)
   - `UPLOAD_RATE_LIMITER` (Limit: 10, Period: 60s)

---

## 8. Initial Principal / Administrator Setup

To initialize the root administrator without exposing credentials:

1. Connect to backend container or execute via administrative CLI tool:
   ```bash
   ./operator-cli create-principal \
     --school-slug="dhadhana" \
     --username="principal" \
     --temporary-password="<strong-temporary-password>"
   ```
2. Log in at `https://<project-name>.pages.dev/admin/login` and immediately update the credentials under **Account Settings**.

---

## 9. Verification & Smoke Tests

Verify each core flow in order:

| Test Case | Method | Expected Output |
|---|---|---|
| Direct Route Refresh | Browse directly to `/about` or `/notices` and reload | HTTP 200, page loads (SPA `_redirects` verified) |
| Public Exam Results | Search for seat number + solve Turnstile | Marksheet rendered; Turnstile verified server-side |
| Admin Login | Submit username/password + Turnstile | Auth cookie issued, redirect to `/admin/dashboard` |
| Gateway Header Stripping | Send `X-Portal-Client-IP: spoofed` to `/api/*` | Stripped and replaced with legitimate `CF-Connecting-IP` |
| Security Headers | Run `curl -I https://<project>.pages.dev` | `X-Content-Type-Options`, `Content-Security-Policy` present |
| Rate Limit Enforcement | Rapidly hit `/api/v1/admin/auth/login` >10 times | HTTP 429 with `Retry-After: 60` and Gujarati error payload |

---

## 10. Rollback Procedure

If a breaking defect is identified post-deploy:
1. In Cloudflare Dashboard, open **Workers & Pages** -> `<project>` -> **Deployments**.
2. Identify the previous stable deployment.
3. Click the three dots `...` next to the deployment and click **Rollback to this deployment**.
4. Confirm rollback in the dialog. Routing reverts globally within seconds.
