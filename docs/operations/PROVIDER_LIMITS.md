# Provider limits relevant to optional caching

**Last checked:** 2026-10-22
Provider plans and limits change; verify the linked official pages again before configuring or provisioning a service. This document does not authorize creating services, adding payment methods, enabling paid plans, or changing billing.

| Provider / option | Limits and operational behavior checked | Relevance to this platform |
|---|---|---|
| Render Free Key Value | One Free Key Value instance per workspace. In-memory only; restarts and maintenance can erase data, and upgrading loses its data. Render describes Free services as intended for testing/hobby use, not production. New Key Value instances run Valkey and support Redis-compatible clients. | A cache may lose all entries at any time; application must tolerate an unavailable or empty remote cache. It adds a remote dependency to the single Render Free JVM. Not selected by [ADR: Defer Redis](../architecture/ADR_REDIS_DECISION.md). |
| Upstash Redis Free | Current pricing page lists 256 MB data size, 500,000 commands/month, 10 GB/month bandwidth, maximum 10,000 commands/second, one Free database, and $0. The page says entering a credit card upgrades the database to Pay as You Go; its budget control is only for Pay as You Go. | Proposed decision ceiling is 250,000 billable commands/month, 128 MiB data, and 5 GiB bandwidth to retain 50% quota headroom. These are policy gates, not observed usage. No account or database was created. No card, billing upgrade, or auto-upgrade is permitted for the free-plan path. |
| Neon PostgreSQL Free | The platform's target database is Neon. Verify current per-project compute-hour, storage, and scale limits on the pricing page before future capacity decisions; exact limits are intentionally not copied here because they are plan-dependent and may change. | PostgreSQL remains the authoritative data store. Cache sizing does not change database truth, and a cache miss must issue the existing tenant-scoped read. |
| Cloudflare Workers | Existing public edge caching is per deployed Worker cache behavior; workers.dev does not provide the same zone-wide cache behavior as a custom-domain CDN zone. Review current Workers and Cache API limits before relying on edge hit-rate assumptions. | Edge cache remains the first layer for eligible public GETs. Do not count edge cache entries as shared across all POPs or assume a hit rate without instrumentation. |
| Render Free web service | Free web services spin down after 15 minutes idle and take about one minute to spin back up; they are single-instance and have no private network traffic support. | Redis does not prevent a Render cold start. A remote cache cannot be reached while the service is stopped and may add latency after restart. |

## Official provider references

- [Render Free services and Key Value limitations](https://render.com/docs/free)
- [Render Key Value compatibility and connection behavior](https://render.com/docs/key-value)
- [Upstash Redis pricing, quota, and billing FAQ](https://upstash.com/pricing/redis)
- [Neon pricing](https://neon.com/pricing)
- [Cloudflare Workers Cache API](https://developers.cloudflare.com/workers/runtime-apis/cache/)

## Operational guardrails

- Do not store private student data, results, sessions, CSRF tokens, credentials, or unpublished content in a shared cache.
- Treat Redis/Key Value as disposable cache state. PostgreSQL remains authoritative.
- A quota or provider outage must bypass the cache and preserve normal tenant-scoped database reads.
- Do not provision a service, add payment details, enable paid upgrades, or change production configuration as part of a documentation decision.
