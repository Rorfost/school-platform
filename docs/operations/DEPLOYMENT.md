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

Render Free can have availability limitations. The application must not depend on Render-specific code, so it can move to another standard container/JVM host without redesign. Before launch, configure production CORS, Secure session cookies, HTTPS, health checks, limited Actuator exposure, backups, object-storage buckets/access, and domain/TLS routing.
