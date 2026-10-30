# Frontend — Rorfost School Platform

The frontend is a React 19, TypeScript, and Vite single-page application and Cloudflare Pages Functions proxy providing a Gujarati-first public portal and an administrative panel for school principals.

- **Platform Product:** Rorfost School Platform
- **Production Deployment:** Dhadhana Primary School Portal (`dhadhana-school-pages.pages.dev`)

## Architecture Overview

- **Hosting & Serving:** Hosted on Cloudflare Pages (`dhadhana-school-pages`) with static assets served from Cloudflare's global edge network.
- **Client-Side Routing:** Managed by React Router with `_redirects` (`/* /index.html 200`) providing seamless direct page refreshes.
- **API Proxy (Pages Functions):** Handled via `functions/api/[[catchall]].ts`. Proxies `/api/*` to the Spring Boot backend on Render.
- **Gateway Security:** Injects `X-Portal-Gateway-Token` (from encrypted Pages runtime secret) and `X-Portal-Client-IP` (from `CF-Connecting-IP`), stripping any client-supplied spoofed headers. Fails closed with HTTP 500 if the gateway token is unset.
- **Dynamic Branding:** School name and logo derived at build-time (`VITE_SCHOOL_NAME`, `VITE_SCHOOL_LOGO`) or fetched dynamically via API.
- **Anti-Bot Protection:** Cloudflare Turnstile integration on public result lookups and principal login (`VITE_TURNSTILE_SITE_KEY`).
- **Feature Flag Awareness:** UI navigation, sidebar, mobile drawers, student shortcuts, and route guards (`FeatureRouteGuard`) conditionally rendered based on active school feature flags fetched from `/api/v1/public/features`.

## Commands

```bash
npm install
npm run dev
npm run lint
npm run format:check
npm run typecheck
npm run test
npm run build
```

## Cloudflare Pages Configuration & Deployment

The frontend uses Cloudflare Pages with project name `dhadhana-school-pages` configured in `wrangler.toml`.

### Build Settings

- **Framework preset:** None (or Vite)
- **Root directory:** `frontend`
- **Build command:** `npm run build`
- **Build output directory:** `dist`

### Secret Provisioning

The Pages Function requires `PORTAL_GATEWAY_TOKEN` provisioned as a runtime secret:

```bash
npx wrangler pages secret put PORTAL_GATEWAY_TOKEN --project-name=dhadhana-school-pages
```

### Manual Deployment via GitHub Actions or CLI

To deploy production assets:

```bash
npm run build
npx wrangler pages deploy dist --project-name=dhadhana-school-pages
```

Alternatively, use the GitHub Actions workflow `.github/workflows/deploy-frontend.yml` via `workflow_dispatch`.

## Directory Structure

- `src/app` — Application routes (`AppRouter.tsx`) with `FeatureRouteGuard` and root providers.
- `src/api` — HTTP client (`client.ts`), CSRF handling, and TypeScript DTOs (`types.ts`).
- `src/components` — Shared UI elements, layout headers/footers, and admin shell.
- `src/pages` — Public pages and `src/pages/admin/` principal views.
- `src/features/` — Cohesive domain features:
    - `auth` — Principal login, session restoration, and route guards.
    - `public` — Result marksheet viewer and public content components.
    - `school` — School metadata and settings hooks.
    - `tools` — Gujarati educational calculators.
    - `featureFlags` — Client-side feature flag gates (`api`, `hooks`, `types`, `defaults`, `FeatureRouteGuard`).
- `functions/` — Cloudflare Pages Functions:
    - `functions/api/[[catchall]].ts` — API gateway proxy with path validation, header sanitization, token injection, and edge caching.
    - `functions/cache/` — Shared edge cache evaluation and key generation utilities.
