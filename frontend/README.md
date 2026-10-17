# Frontend — Rorfost School Platform

The frontend is a React 19, TypeScript, and Vite single-page application and Cloudflare Worker proxy providing a Gujarati-first public portal and an administrative panel for school principals.

- **Platform Product:** Rorfost School Platform
- **Reference Deployment (As-Is):** Dhadhana Primary School Portal

## As-Is vs. Target Architecture

### Current State (As-Is)

- Single Cloudflare Worker deployment (`dhadhana-school`).
- Hardcoded fallback school metadata (`DEFAULT_SCHOOL_INFO`) and Dhadhana footer credits in local fallback utilities.
- Cloudflare Worker proxies `/api/*` directly without cryptographic gateway authentication.

### Target State (Multi-Tenant Plan)

- Unified codebase deployed to independent named Cloudflare Workers per school (`school-a`, `school-b`, `school-c`).
- Each Worker holds a unique `PORTAL_GATEWAY_TOKEN` secret and injects it into upstream `/api/*` requests to Render.
- Dynamic branding: School name, logo, principal information, and footer copyright derived strictly from API data, with generic neutral defaults.
- Feature Flag Awareness: UI navigation and page elements conditionally rendered based on active school feature flags.

## Features Implemented

- **Public School Website (Gujarati):** Home, About School, Principal's Desk, Notices, Photo Gallery, Downloads, and Contact.
- **Student Corner (Gujarati):** Study Materials, Examination Results (with privacy PIN verification, marksheet display, and single-page A4 print), and Downloads.
- **School Tools (Gujarati):** Client-side educational and administrative calculators (age, attendance, marks percentage).
- **Principal Admin Panel (English):**
    - Session authentication, CSRF handling, password change lifecycle.
    - Operational Dashboard with school overview.
    - School Settings & Principal Profile with ImageKit-backed logo management.
    - Academic setup: Academic years, standards, subjects, and standard-subject mapping.
    - Results & Marks: Assessment setup, Excel import (Annual & Ekam Kasoti), and publish controls.
    - Content management: Notices, study materials, photo gallery albums, and downloads.

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

## Directory Structure

- `src/app` — Application routes (`AppRouter.tsx`) and root providers.
- `src/api` — HTTP client (`client.ts`), CSRF handling, and TypeScript DTOs (`types.ts`).
- `src/components` — Shared UI elements, layout headers/footers, and admin shell.
- `src/pages` — Public pages and `src/pages/admin/` principal views.
- `src/features/` — Cohesive domain features:
    - `auth` — Principal login, session restoration, and route guards.
    - `public` — Result marksheet viewer and public content components.
    - `school` — School metadata and settings hooks.
    - `tools` — Gujarati educational calculators.
    - `tenant` — Planned tenant context and multi-school routing awareness (Sprint 01).
    - `featureFlags` — Planned client-side feature flag gates (Sprint 01).
- `worker/` — Cloudflare Worker script (`index.ts`) proxying same-origin API requests to the backend.
