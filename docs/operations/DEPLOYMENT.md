# Deployment Guide — Rorfost School Platform

## Deployment Topologies: As-Is vs. Target

### Current Production State (As-Is)
- **Frontend:** Single Cloudflare Worker (`dhadhana-school.workers.dev`) serving static SPA and proxying `/api/*`.
- **Backend:** One Render Free service (`school-platform`).
- **Database:** Aiven PostgreSQL.
- **Media:** ImageKit single storage bucket.

### Target Multi-Tenant State (Plan)
- **Frontend:** Three (or more) independent named Cloudflare Workers (`school-a`, `school-b`, `school-c`), each with its own `PORTAL_GATEWAY_TOKEN` secret binding.
- **Backend:** Single shared Spring Boot instance on Render Free running in production profile (`SPRING_PROFILES_ACTIVE=prod`).
- **Database:** Single shared Neon PostgreSQL database (`school-platform-prod`), pooled and connection-bounded (`DB_POOL_MAX_SIZE=3`).
- **Media:** ImageKit single account with school-specific namespace prefixes (`schools/<school-uuid>/...`).

## Freeze Safeguards Before Migration

Prior to applying any multi-tenant code changes or schema migrations:
1. **Render Auto-Deploy:** In **Render → Service → Settings → Auto-Deploy**, set to **Off**. Record the running commit SHA.
2. **Cloudflare Builds:** In **Cloudflare → Workers & Pages → Settings → Builds**, disable automatic production deployments or set build command to `npx wrangler versions upload`.
3. **GitHub CI:** Keep GitHub branch protection active. A Git branch does not prevent automated deploys if provider webhooks are enabled.

## Environment Variable Architecture

- **Backend (Render Secrets):** `DB_URL` (Neon JDBC string with `sslmode=require`), `DB_USERNAME`, `DB_PASSWORD`, `IMAGEKIT_PRIVATE_KEY`, `SESSION_COOKIE_SECURE=true`.
  *(Note: Render does not store plaintext portal gateway tokens; token digests are stored in Neon.)*
- **Cloudflare Workers:**
  - `BACKEND_ORIGIN`: Non-secret URL pointing to the Render backend service (e.g. `https://your-service.onrender.com`).
  - `PORTAL_GATEWAY_TOKEN`: Secret cryptographically random token injected into `X-Portal-Gateway-Token` on `/api/*` proxies.
- **Frontend Build (`VITE_*`):** Public build variables only. Never inject secrets into `VITE_*` environment variables.

## Provider Free-Tier Limits & Reality

- **Render Free:** 750 shared instance-hours/month; instances sleep after ~15 minutes of inactivity; 512 MB RAM / 0.1 CPU limit. Cold starts are expected; 100% uptime is not guaranteed.
- **Neon Free:** Subject to compute hour and storage bounds; verify connection pooling parameters (`DB_POOL_MAX_SIZE=3`).
- **Cloudflare Workers Free:** 100,000 requests/day per account.
- **No Card / Zero Overages:** The platform is configured to stay within free tier thresholds without triggering paid overages.

## Manual Release Workflows & CI/CD Safety Controls (Sprint 26)

To guarantee that branch pushes or merged PRs never trigger unexpected live changes to School 1 production:

1. **Auto-Deploy Disablement Enforced:**
   - **Render:** Auto-deploy is permanently set to **Off**. Production backend updates require explicit manual triggering via `.github/workflows/deploy-backend.yml` (`workflow_dispatch`) with confirmation string `DEPLOY`.
   - **Cloudflare Workers:** Git integration / automatic build triggers are set to **Off**. Worker deployments require manual triggering via `.github/workflows/deploy-frontend.yml` with explicit `target_school` selection (`school_a`, `school_b`, `school_c`, or `dhadhana_prod`).

2. **Secret Hygiene & Least-Privilege Architecture:**
   - **No Plaintext Secrets in Tracking:** Neither raw `PORTAL_GATEWAY_TOKEN` secrets nor Neon database connection URIs are committed to version control. Automated CI job `secret-hygiene` scans every push and PR to reject sensitive keys.
   - **Runtime Database Privilege:** Render backend connects to Neon using a restricted application user (`neondb_app`), never the root `neondb_owner` superuser.
   - **Cloudflare API Scoping:** Deployment credentials (`CLOUDFLARE_API_TOKEN`) are scoped strictly to Worker script editing permissions on the specific account.
   - **Wrangler Secret Management:** `wrangler secret put` must never be run interactively on production workers without human change-window authorization, as setting a secret immediately generates and deploys a new worker version.

3. **Release Ledger & Immutable SHA Tracking:**
   Every manual production promotion must record the exact deployed commit SHA across all active workers, the backend service, and Flyway migration version in `docs/operations/MIGRATION_BASELINE.md`.

