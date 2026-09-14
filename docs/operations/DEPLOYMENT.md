# Deployment

Current production direction:

| Component | Initial service |
| --- | --- |
| Frontend | Cloudflare |
| Backend | Render Free initially |
| Database | Aiven PostgreSQL |
| Object storage | Cloudflare R2 |
| DNS/CDN/TLS | Cloudflare |

Expected domain shape is `school-domain.example`, `api.school-domain.example`, and `assets.school-domain.example`; no real domain is selected yet. Deploy frontend static assets through Cloudflare, run the backend as a standard JVM/container service, connect it privately/securely to managed PostgreSQL and R2, and inject production secrets through provider configuration.

For Cloudflare Pages, configure the monorepo root as `frontend`, build command as `npm run build`, and output directory as `dist`, following Cloudflare's current [build configuration](https://developers.cloudflare.com/pages/configuration/build-configuration/). The React/Vite app is a static SPA; Cloudflare Pages provides SPA route fallback when no top-level `404.html` is deployed. The `public/_headers` file provides only safe static response headers and does not replace backend API security.

Render Free can have availability limitations. The backend Dockerfile uses a standard Java 21 multi-stage build and has no Render-specific logic, so it can move to another standard container/JVM host without redesign. Before launch, configure production CORS, Secure session cookies, HTTPS, health checks, limited Actuator exposure, backups, object-storage buckets/access, and domain/TLS routing.
