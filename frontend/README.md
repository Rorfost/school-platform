# Frontend

The frontend is a React 19, TypeScript, and Vite static single-page application. It provides only the Phase 0 shell: public layout, Gujarati placeholder home, not-found route, query provider, API client, and shared feedback primitives. Product pages and school-specific content are intentionally deferred.

## Commands

```bash
npm install
npm run dev
npm run lint
npm run typecheck
npm run test
npm run build
```

Copy `.env.example` to `.env.local` for local values. Every `VITE_*` value is embedded in the browser bundle and is public; never put secrets there.

## Structure

- `src/app` — application providers and route configuration
- `src/api` — shared HTTP client foundation
- `src/components` — shared layout, UI, and feedback components
- `src/pages` — route-level pages
- `src/features` — future feature-owned UI; add a feature only when work starts
- `src/styles` — global design tokens and base styling

## Deployment

Cloudflare Pages is the intended frontend host. Configure the Pages project with repository root `frontend`, build command `npm run build`, and build output `dist`. The application is a static SPA; no frontend Docker deployment is required.
