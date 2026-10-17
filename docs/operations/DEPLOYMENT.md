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
