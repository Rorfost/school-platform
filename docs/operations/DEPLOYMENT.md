# Deployment Guide — Rorfost School Platform

## Deployment Topologies: As-Is vs. Target

### Current Production State (As-Is)
- **Frontend:** Cloudflare Pages (`dhadhana-school-pages.pages.dev`) serving static SPA assets and proxying `/api/*` via Pages Functions.
- **Backend:** One Render Web Service (`school-platform-gxrt.onrender.com`).
- **Database:** Neon Serverless PostgreSQL.
- **Media:** ImageKit single storage bucket.

### Target Multi-Tenant State (Production Architecture)
- **Frontend:** Cloudflare Pages with Pages Functions gateway (`dhadhana-school-pages`), holding the `PORTAL_GATEWAY_TOKEN` secret binding.
- **Backend:** Shared Spring Boot instance on Render running in production profile (`SPRING_PROFILES_ACTIVE=prod`).
- **Database:** Shared Neon PostgreSQL database (`school-platform-prod`), connection-bounded (`DB_POOL_MAX_SIZE=3`).
- **Media:** ImageKit single account with school-specific namespace prefixes (`schools/<school-uuid>/...`).

## Freeze Safeguards Before Migration

Prior to applying any multi-tenant code changes or schema migrations:
1. **Render Auto-Deploy:** In **Render → Service → Settings → Auto-Deploy**, set to **Off**. Record the running commit SHA.
2. **Cloudflare Builds:** In **Cloudflare → Workers & Pages → Settings → Builds**, disable automatic production deployments or manage deployments via GitHub Actions.
3. **GitHub CI:** Keep GitHub branch protection active. A Git branch does not prevent automated deploys if provider webhooks are enabled.

## Environment Variable Architecture

- **Backend (Render Secrets):** `DB_URL` (Neon JDBC string with `sslmode=require`), `DB_USERNAME`, `DB_PASSWORD`, `IMAGEKIT_PRIVATE_KEY`, `SESSION_COOKIE_SECURE=true`, `TURNSTILE_SECRET_KEY`.
  *(Note: Render does not store plaintext portal gateway tokens; token digests are stored in Neon.)*
- **Cloudflare Pages:**
  - `BACKEND_ORIGIN`: Non-secret URL pointing to the Render backend service (`https://school-platform-gxrt.onrender.com`).
  - `PORTAL_GATEWAY_TOKEN`: Secret cryptographically random token injected into `X-Portal-Gateway-Token` on `/api/*` proxies.
  - `SCHOOL_SLUG`: School identifier (`dhadhana`).
- **Frontend Build (`VITE_*`):** Public build variables only (`VITE_TURNSTILE_SITE_KEY`, `VITE_SCHOOL_NAME`, `VITE_SCHOOL_LOGO`, `VITE_ASSET_BASE_URL`). Never inject secrets into `VITE_*` environment variables.

## Provider Free-Tier Limits & Reality

- **Render Free:** 750 shared instance-hours/month; instances sleep after ~15 minutes of inactivity; 512 MB RAM / 0.1 CPU limit. Cold starts are expected; 100% uptime is not guaranteed.
- **Neon Free:** Subject to compute hour and storage bounds; verify connection pooling parameters (`DB_POOL_MAX_SIZE=3`).
- **Cloudflare Pages Free:** Unlimited requests; Cloudflare Pages Functions free tier provides 100,000 requests/day per account.
- **No Card / Zero Overages:** The platform is configured to stay within free tier thresholds without triggering paid overages.

## Manual Release Workflows & CI/CD Safety Controls

To guarantee that branch pushes or merged PRs never trigger unexpected live changes to production:

1. **Auto-Deploy Disablement Enforced:**
   - **Render:** Auto-deploy is permanently set to **Off**. Production backend updates require explicit manual triggering via `.github/workflows/deploy-backend.yml` (`workflow_dispatch`) with confirmation string `DEPLOY`.
   - **Cloudflare Pages:** Pages direct upload deployments require manual triggering via `.github/workflows/deploy-frontend.yml` with explicit confirmation string `DEPLOY`.

2. **Secret Hygiene & Least-Privilege Architecture:**
   - **No Plaintext Secrets in Tracking:** Neither raw `PORTAL_GATEWAY_TOKEN` secrets nor Neon database connection URIs are committed to version control. Automated CI job `secret-hygiene` scans every push and PR to reject sensitive keys.
   - **Runtime Database Privilege:** Render backend connects to Neon using a restricted application user (`neondb_app`), never the root `neondb_owner` superuser.
