# Deployment

Current production direction:

| Component | Initial service |
| --- | --- |
| Frontend | Cloudflare Workers |
| Backend | Render Free initially |
| Database | Aiven PostgreSQL |
| Object storage | ImageKit |
| DNS/CDN/TLS | Cloudflare |

Expected domain shape is `school-domain.example`, `api.school-domain.example`, and `assets.school-domain.example`; no real domain is selected yet. Deploy frontend static assets through Cloudflare, run the backend as a standard JVM/container service, connect it privately/securely to managed PostgreSQL and ImageKit, and inject production secrets through provider configuration.

The frontend uses the checked-in Cloudflare Worker configuration in `frontend/wrangler.jsonc`. Run `npm run build` to create `dist`, then deploy with `wrangler deploy` through an authenticated provider environment. The Worker serves static SPA assets and proxies `/api/` requests to the configured backend origin. The `public/_headers` file provides only safe static response headers and does not replace backend API security.

Render Free can have availability limitations. The backend Dockerfile uses a standard Java 21 multi-stage build and has no Render-specific logic, so it can move to another standard container/JVM host without redesign. Before launch, configure production CORS, Secure session cookies, HTTPS, health checks, limited Actuator exposure, backups, object-storage buckets/access, and domain/TLS routing.
