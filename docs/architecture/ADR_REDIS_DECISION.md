# ADR: Defer Redis for the current deployment

**Status:** DEFER
**Decision date:** 2026-10-22
**Scope:** One Render Free JVM, one shared Neon PostgreSQL database, Cloudflare Workers, and ImageKit. No Redis service is provisioned by this decision.

## Context

Sprints 31–40 added query profiling and bounded caching opportunities. Sprint 40 introduced a 30-second, 128-school in-process cache for the public feature snapshot. The cache is keyed by the gateway-verified school UUID, does not cache authorization decisions or private data, and evicts after a successful write commits.

The available Sprint 40 benchmark is a synthetic JVM microbenchmark with a mocked repository. Across three rounds of 20,000 measured reads after 5,000 warmups, the median warm-cache result was p50/p95 0.1/0.1 µs, 32.5 allocated bytes per operation, zero observed GC collections, and zero repository reads. The uncached median was p50/p95 4.5/11.0 µs, 3,181.7 allocated bytes per operation, three collections / 17 ms, and one repository read per operation. This demonstrates local lookup overhead and avoided mock repository calls; it does not measure Neon latency, HTTP route latency, Render CPU or heap pressure, real cache hit rate, or production traffic.

Those deployment measurements are currently **not available**. The feature route itself was not measured in Sprint 31, and there is no observed per-route request volume, hot-query p95, hit/miss rate, JVM resource series, or peak request profile. Redis command volume cannot be predicted from the evidence without inventing request counts.

## Decision

**Defer Redis.** Continue using the existing bounded in-process feature snapshot cache and the existing Cloudflare public-response edge cache. Keep PostgreSQL as the source of truth. Do not add a Redis dependency, service, credentials, or runtime configuration in this sprint.

A remote cache would add a network hop, another credential and failure domain, tenant-aware invalidation requirements, and provider quotas. With one JVM, the local cache avoids remote round trips and already bounds memory. Redis cannot repair unindexed or inefficient SQL, unbounded upload work, or Render cold starts. A free external cache is not itself a production reliability or scaling plan.

## Alternatives considered

| Option | Current role and trade-off | Decision |
|---|---|---|
| Cloudflare Worker edge cache | Already caches allowlisted public GET responses at each Worker edge location. It can avoid reaching Render on a hit; cache state is not globally shared across Worker locations. Sensitive, authenticated, cookie-bearing, and mutation responses are excluded. | Keep for eligible public responses. |
| Bounded in-process cache | Sprint 40 provides a 128-school LRU and 30-second TTL for one small public feature snapshot. No network hop, and the value disappears safely on process restart. It does not share entries across JVMs. | Keep; already implemented. |
| PostgreSQL | Authoritative store; a tenant-scoped query is needed on a local cache miss. Query count and p95 should be measured before introducing another store. | Keep as source of truth. Tune SQL based on measured plans. |
| Render Key Value | Redis-compatible service (new instances use Valkey). A Free instance is in-memory only and may restart, losing all data. It would still be remote to the application and introduces a service dependency. | Reject for the current single-instance free deployment. |
| Hosted Redis such as Upstash | Free plan currently advertises 256 MB, 500,000 monthly commands, and 10 GB monthly bandwidth. Its Free allowance has no charge, but quotas and terms can change; the documented PAYG budget guard applies only after upgrading. | Do not provision. Re-evaluate only with measured demand and an explicit owner decision. |

Provider limits were checked on 2026-10-22. See [Provider limits](../operations/PROVIDER_LIMITS.md) and the linked provider documentation there.

## Evidence gaps and measurement needed

Before reconsidering, collect at least seven consecutive days of synthetic staging observations across three tenant profiles, with cold and warm traffic separated. Record route request counts and p50/p95/p99, cache hit/miss/eviction counts, DB calls per request, hot-query p95, Render CPU/heap/GC, and request spikes. Do not run load tests against School 1 production or collect student records.

The following proposed gates are deliberately conservative decision thresholds, not current measurements:

1. A cacheable route remains a demonstrated bottleneck after query/index and edge-cache review: the database portion is at least 5 ms or 20% of route p95, and it generates at least 100,000 avoidable tenant-scoped reads per month.
2. The in-process cache has a measured hit rate below 50% because of a documented multi-instance or churn requirement, or measured process memory/GC pressure prevents a safe local cap. A cache miss must still be safe against PostgreSQL.
3. A same-region staging comparison demonstrates at least 20% p95 improvement versus the existing local/edge/PostgreSQL path, with no tenant-isolation or availability regression. Redis round-trip p95 must be lower than the database work it avoids.
4. Forecast cache footprint is at most 128 MiB of a 256 MiB allowance and transfer at most 5 GiB of a 10 GiB allowance, leaving at least 50% headroom.
5. Forecast usage is at most 250,000 billable commands/month, half the currently listed Free allowance, and peak command rate is at most 5,000/s, half the currently listed 10,000/s limit. Keep the free, no-payment-method path; never enable paid upgrade or billing as an automatic fallback.

If these gates are not all evidenced, retain the DEFER decision. The thresholds are revalidated against provider terms before any future provisioning.

## Command and resource model

For a future read-through cache with one `GET` for each eligible backend request, one `SET` for each miss, and one `DEL` for each successful invalidating write:

```text
monthly_commands = eligible_backend_requests + cache_misses + invalidating_writes
```

Let `R`, `M`, and `W` be the observed monthly counts for those events. Projected use is `R + M + W`. Current values for all three are **unknown**, so a numeric monthly projection is **not supportable**. If the future design adds retries, key-version reads, or multi-command writes, count each billable operation separately. Do not count client bootstrap/connection commands as reads without checking the then-current provider billing definition.

At the proposed go/no-go ceiling of 250,000 commands/month, use of the presently listed Upstash Free allowance would remain at or below 50%; this is a policy ceiling, not a forecast. Exceeding the ceiling means no Free deployment. No PAYG or automatic plan change is allowed by this ADR.

## Security, tenant isolation, and failure behavior

No student results, sessions, CSRF values, authentication state, admin/private data, unpublished content, or credentials may enter any shared cache. Any future cache key must include the immutable school UUID obtained from verified server tenant context, plus an explicit namespace/version and every value dimension affecting the result. Never derive a cache identity from client-supplied `schoolId`, slug, `Host`, or arbitrary headers.

Any future cache outage, timeout, malformed value, or open circuit must fall back to the bounded local cache where applicable and then to the tenant-scoped PostgreSQL source of truth. Cache availability must never determine authorization or fail an otherwise valid public read. Any write invalidation must happen only after the database transaction commits. A future implementation requires contract tests for cross-tenant key separation, TTL/size bounds, invalidation, timeout/circuit behavior, and free-plan billing gates. No adapter or contract tests are warranted while this ADR is DEFER.

## Reconsideration trigger

Reopen this ADR only after the measurement set above demonstrates every numeric gate, the provider quota review is current, and the owner explicitly approves the optional experiment. Sprint 42 remains skipped unless a later accepted ADR and owner approval authorize it.

**Recorded decision:** DEFER Redis; retain the existing edge and bounded local caches.
**Deployment:** None.
