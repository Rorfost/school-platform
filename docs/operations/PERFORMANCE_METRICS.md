# Performance Metrics — Operations Guide

**Scope:** Backend request timing, slow-request diagnostics, and Actuator health.  
**Last updated:** Sprint 32 — Backend timing and low-overhead metrics.

---

## Overview

The backend exposes **zero new public endpoints** for metrics.  
Actuator remains locked to `/actuator/health` only (unauthenticated, JSON).  
All instrumentation is **off by default** and enabled via environment variables.

---

## Request timing — what is always logged

Every non-test request produces a structured INFO log line:

```
request_complete requestId=<uuid> method=GET routeClass=public.get statusClass=2xx status=200 durationMs=47
```

### Bounded route-class labels

Raw URI path segments (student IDs, school slugs, roll numbers) are **never** written to log
fields or Micrometer tags. The vocabulary is intentionally small:

| `routeClass`      | Matches                                               |
|-------------------|-------------------------------------------------------|
| `public.get`      | `GET /api/v1/public/**`                               |
| `public.mutation` | `POST/PUT/PATCH/DELETE /api/v1/public/**`             |
| `admin.get`       | `GET /api/v1/admin/**`                                |
| `admin.mutation`  | `POST/PUT/PATCH/DELETE /api/v1/admin/**`              |
| `actuator`        | `/actuator/**` — excluded from business metrics       |
| `other`           | Everything else (static assets, unknown paths)        |

`statusClass` is one of `2xx`, `3xx`, `4xx`, `5xx`, `other`.

---

## Opt-in Micrometer timers (Task A)

| Environment variable                       | Default | Purpose                                               |
|--------------------------------------------|---------|-------------------------------------------------------|
| `APP_METRICS_TIMERS_ENABLED`               | `false` | Enable Micrometer `http.server.requests.custom` timer |

When enabled, two meters are registered with bounded tags:

- **`http.server.requests.custom`** — histogram timer; tags: `route_class`, `method`, `status_class`.
- **`http.server.requests.slow`** — counter; tags: `route_class`, `method`. Incremented for sampled slow requests.

> **Free-tier guidance:** Do **not** enable timers on Render Free unless a Micrometer scraper
> (e.g., Prometheus `/actuator/metrics` scrape target) is attached. No such endpoint is exposed.
> Timers are valuable for local or staging load-testing runs only.

---

## Opt-in slow-request diagnostic (Task B)

| Environment variable                             | Default  | Purpose                                           |
|--------------------------------------------------|----------|---------------------------------------------------|
| `APP_METRICS_SLOW_REQUEST_LOGGING_ENABLED`       | `false`  | Enable WARN log for requests above threshold      |
| `APP_METRICS_SLOW_REQUEST_THRESHOLD`             | `2000ms` | Threshold (Spring `Duration` format)              |
| `APP_METRICS_SLOW_REQUEST_SAMPLE_RATE`           | `0.1`    | Fraction of qualifying requests to log (0.0–1.0)  |

When a slow request fires, a WARN line is written:

```
slow_request requestId=<uuid> method=POST routeClass=admin.mutation statusClass=2xx durationMs=3210 thresholdMs=2000
```

**No personal data appears in this line:** student roll numbers, school slugs, email addresses and
credentials are never written to the log.

### Collecting a before/after staging sample

1. Deploy with `APP_METRICS_SLOW_REQUEST_LOGGING_ENABLED=true` and
   `APP_METRICS_SLOW_REQUEST_THRESHOLD=500ms`.
2. Run the synthetic load scenario from `docs/operations/LOAD_AND_RESOURCE_VALIDATION.md`.
3. Grep staging logs for `slow_request` lines:
   ```
   grep slow_request app.log | awk '{print $6, $8}' | sort -n -k2
   ```
4. Compare `durationMs` distributions before and after a change.
5. Disable (`APP_METRICS_SLOW_REQUEST_LOGGING_ENABLED=false`) before restoring to Render Free.

> **Log cardinality note:** With default 10 % sample rate and 2 s threshold, expected log
> volume is negligible on typical school-portal traffic (< 1 000 req/day on free tier).

---

## Actuator health endpoint

```
GET /actuator/health
```

Returns `{"status":"UP"}` (or `DOWN`) without details. Accessible by unauthenticated public
callers — used by Render Free health checks. No authentication, session, or CSRF needed.

No other Actuator endpoints are exposed (`management.endpoints.web.exposure.include: health`).

---

## What is NOT done (Explicit exclusions)

- No Prometheus scrape endpoint exposed.
- No Grafana, tracing SaaS, or APM agents installed.
- No `/actuator/metrics` endpoint exposed to the public.
- No paid telemetry service or auto-upgrade triggered.
- No Redis, distributed tracing, or external metric store.

---

## Rollback / disable switch

Set these environment variables to `false` (or remove them) to fully disable all instrumentation:

```
APP_METRICS_TIMERS_ENABLED=false
APP_METRICS_SLOW_REQUEST_LOGGING_ENABLED=false
```

No restart of the database or Cloudflare Workers is needed; only a backend redeploy.

---

## Metric limitations on free services

| Service       | Limitation                                                            |
|---------------|-----------------------------------------------------------------------|
| Render Free   | No persistent metrics storage; logs expire after ≈ 7 days            |
| Neon Free     | No query-plan history; use `EXPLAIN ANALYZE` manually via psql        |
| Cloudflare    | Workers analytics available in dashboard; not correlated with backend |

For load testing, use the local Docker Compose setup described in
`docs/operations/LOCAL_DEVELOPMENT.md` with `APP_METRICS_TIMERS_ENABLED=true`.
