# Cache Invalidation Runbook (Sprint 39)

**Document Version:** 1.0  
**Repository:** https://github.com/Rorfost/school-platform  
**Scope:** Cloudflare Worker edge cache — invalidation strategy, TTL tables, purge procedures, and emergency rollback.

> [!NOTE]
> The Workers Cache API (`caches.default`) does **not** expose a programmatic purge API on the free plan.  
> Invalidation strategy is **TTL-first**: conservative short TTLs are the primary mechanism.  
> For content that must be fresh immediately, use the **version-bump** procedure below.

---

## 1. How the Edge Cache Works

Each school's Cloudflare Worker is a **separate Worker instance** (e.g., `dhadhana-school`, `school-a-portal`).  
The Worker uses `caches.default` (Workers Cache API) to cache allowlisted `GET` responses per-school.

**Cache key format:**

```
https://cache.internal/{schoolSlug}{pathname}{queryString}
```

Examples:
- `https://cache.internal/dhadhana/api/v1/public/school`
- `https://cache.internal/school-a/api/v1/public/notices?page=1&size=20`
- `https://cache.internal/school-b/api/v1/public/gallery/albums`

**Critical properties:**
- Each school slug is URL-encoded in the key → zero cross-tenant key collision.
- Pagination params (`?page=N&size=M`) are part of the key → paginated responses are independently cached.
- `caches.default` on `workers.dev` is per-edge-node (not zone-wide). A cold edge node will miss and re-fetch. This is safe — it never serves stale data from another school.

---

## 2. Allowlist and TTL Table

Only the routes below are ever cached. Any route NOT on this list receives `no-store` and is never stored.

| Route | Category | Edge TTL | Stale-While-Revalidate |
|---|---|---|---|
| `GET /api/v1/public/school` | Semi-Static | **300 s (5 min)** | 30 s |
| `GET /api/v1/public/principal-profile` | Semi-Static | **300 s (5 min)** | 30 s |
| `GET /api/v1/public/features` | Semi-Static | **180 s (3 min)** | 30 s |
| `GET /api/v1/public/result-settings` | Semi-Static | **300 s (5 min)** | 30 s |
| `GET /api/v1/public/academic/standards` | Semi-Static | **600 s (10 min)** | 30 s |
| `GET /api/v1/public/academic/standards/{id}/subjects` | Semi-Static | **600 s (10 min)** | 30 s |
| `GET /api/v1/public/notices` (paginated) | Dynamic | **60 s (1 min)** | 30 s |
| `GET /api/v1/public/materials` (paginated) | Dynamic | **60 s (1 min)** | 30 s |
| `GET /api/v1/public/gallery/albums` (paginated) | Dynamic | **60 s (1 min)** | 30 s |
| `GET /api/v1/public/gallery/albums/{id}/images` | Dynamic | **60 s (1 min)** | 30 s |
| `GET /api/v1/public/downloads` (paginated) | Dynamic | **60 s (1 min)** | 30 s |

**NEVER cached (enforced at code level):**

| Route | Reason |
|---|---|
| `GET /api/v1/public/exam-results` | Personal student data — `no-store` mandatory |
| `GET /api/v1/public/visits` | Real-time counter |
| `POST /api/v1/public/visits` | Mutation |
| `POST /api/v1/admin/auth/login` | Sets `Set-Cookie` (JSESSIONID) |
| `GET /api/v1/admin/auth/csrf` | Dynamic CSRF token |
| `GET /api/v1/admin/**` | All admin endpoints — session-bound |
| Any response with `Set-Cookie` | Session fixation invariant — bypassed unconditionally |
| Any non-200 response | Only successful public responses are cached |

---

## 3. Normal Content Freshness (TTL-Based Invalidation)

**For Dynamic routes (notices, materials, gallery, downloads) — 60 s TTL:**

Stale content resolves within **60–90 seconds** without any action. This covers:
- Admin publishes, pins, or unpins a notice
- Admin uploads or removes a study material
- Admin adds or reorders gallery images
- Admin publishes or removes a download

**No action required** for routine content changes. The TTL is intentionally short.

**For Semi-Static routes (school info, branding, feature flags) — 3–10 min TTL:**

Changes made in the Admin → School Settings, Principal Profile, or Feature Flags take up to **10 minutes** to propagate to all cached edge nodes. This is acceptable for branding/config changes.

> [!TIP]
> If the principal needs an urgent branding update to appear immediately, use the **version-bump** procedure below.

---

## 4. Forced Cache Bypass — Version-Bump Procedure

The Workers Cache API does **not** support programmatic purge on the free plan. To force immediate invalidation of any cached route, use one of the following approaches:

### Option A: Re-deploy the Worker (recommended for urgent config changes)

Re-deploying the Worker creates a new isolate, which starts with a cold `caches.default` for that edge node.

```bash
# From the frontend/ directory (after npm run build)
wrangler deploy --env school_a   # for School A
wrangler deploy --env school_b   # for School B
wrangler deploy --env school_c   # for School C
```

> [!CAUTION]
> A Worker re-deploy takes ~30 seconds and briefly causes cache misses (extra backend requests). It does **not** interrupt ongoing requests. Only re-deploy when urgent.

### Option B: API response version bump (for data-layer invalidation)

The backend already sets `ETag` on public GET responses (Sprint 38). When the admin makes a change, the backend generates a new ETag. On the next cache miss after TTL expiry, the Worker fetches a fresh response with the updated ETag.

For forced immediate freshness **without re-deploy**, temporarily reduce the TTL by modifying `CACHEABLE_ROUTES` in [`frontend/worker/cache.ts`](../../frontend/worker/cache.ts):

```typescript
// Example: temporarily reduce notices TTL to 5 seconds for urgent update
{ pathPrefix: "/api/v1/public/notices", exact: true, edgeTtlSeconds: 5 },
```

Then re-deploy, wait 10 seconds, restore the original TTL, and re-deploy again.

### Option C: Disable edge caching entirely (emergency rollback)

See **Section 6** below.

---

## 5. Safety Checklist Before Any Cache Configuration Change

Before modifying the allowlist or TTLs in `cache.ts`:

- [ ] **Confirm route is truly public** — no authentication, no student data, no session dependency
- [ ] **Confirm backend emits `public` Cache-Control** — not `no-store` or `private`
- [ ] **Confirm route never emits `Set-Cookie`** — the bypass guard catches this at runtime, but verify it in the sprint contract tests (`CacheControlContractTest.java`)
- [ ] **Confirm response content is tenant-scoped** — the backend must only return data for the authenticated gateway school, never cross-tenant data
- [ ] **Add or update `matchesCacheableRoute` tests** in `cache.test.ts` to cover the new route
- [ ] **Run full test suite** before deploying: `npm run typecheck && npm run lint && npm run format:check && npm run test`

---

## 6. Emergency Rollback — Disable Edge Caching

If the edge cache is suspected to serve incorrect data, disable it immediately:

**Step 1 — Set flag in `frontend/worker/cache.ts`:**

Add a top-level constant that short-circuits all eligibility checks:

```typescript
// EMERGENCY: set to true to disable all edge caching
const CACHE_DISABLED = true;

export function isCacheEligibleRequest(_request: Request): boolean {
    if (CACHE_DISABLED) return false;
    // ... rest of implementation
}
```

**Step 2 — Re-deploy all affected Workers:**

```bash
npm run build
wrangler deploy --env school_a
wrangler deploy --env school_b
wrangler deploy --env school_c
```

**Step 3 — Verify cache is bypassed:**

Check that responses no longer carry `X-Cache: HIT` headers. All responses should be fresh from the Render backend.

**Step 4 — Investigate and restore:**

Identify the root cause (unexpected `Set-Cookie` on a public route, backend bug, etc.), fix it, remove the `CACHE_DISABLED` flag, and re-deploy.

> [!WARNING]
> Never leave `CACHE_DISABLED = true` in production for more than 30 minutes. The Render free tier has limited concurrency — without the edge cache absorbing repeated public GET requests, cold-start latency increases significantly.

---

## 7. Monitoring Cache Behaviour

The Worker sets `X-Cache: HIT` or `X-Cache: MISS` on every API response for observability.

**To observe cache behaviour manually:**

```bash
# Check a public school endpoint for School A
curl -I https://school-a-portal.workers.dev/api/v1/public/school
# Look for: X-Cache: HIT or X-Cache: MISS

# Verify exam results are never cached
curl -I "https://school-a-portal.workers.dev/api/v1/public/exam-results?rollNumber=12&standard=5"
# Must see: Cache-Control: no-store (no X-Cache header at all)

# Verify admin routes are never cached
curl -I https://school-a-portal.workers.dev/api/v1/admin/auth/me
# Must see: Cache-Control: no-store, no-cache, must-revalidate, private
```

**Expected behaviour:**
- First request to a cacheable route: `X-Cache: MISS`
- Subsequent requests within TTL (same edge node): `X-Cache: HIT`
- Requests to non-cacheable routes: no `X-Cache` header
- Requests to admin/exam routes: `Cache-Control: no-store`

---

## 8. Tenant Isolation Verification

To confirm School A's cache never bleeds into School B:

```bash
# Request school info from School A Worker
curl https://school-a-portal.workers.dev/api/v1/public/school | jq .schoolName
# Expected: "School A Name"

# Request school info from School B Worker (different Worker deployment, different slug)
curl https://school-b-portal.workers.dev/api/v1/public/school | jq .schoolName
# Expected: "School B Name" — MUST be different from School A

# Both workers hit the same backend but use different gateway tokens
# and different cache key prefixes (school-a vs school-b)
```

---

*Document maintained alongside `docs/engineering/CACHE_POLICY.md`. Update this runbook whenever the allowlist, TTLs, or Worker cache logic changes.*
