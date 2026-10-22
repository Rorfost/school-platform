# Multi-Tenant Cache Eligibility & Safety Policy Matrix (Sprint 37)

**Document Version:** 1.1  
**Repository:** https://github.com/Rorfost/school-platform  
**Scope:** Architectural prerequisite establishing route-by-route cache classification, strict multi-tenant keying rules, invalidation boundaries, and `no-store` mandates for student records, credentials, and mutations.

> [!NOTE]
> **Sprint 38 Implementation Status:** Safe backend HTTP response headers (`Cache-Control`) and tenant-scoped `ETag` conditional validation (`304 Not Modified`) have been implemented across all eligible non-sensitive public GET endpoints (`SchoolController`, `PublicContentController`, `PublicFeatureFlagController`, `PublicAcademicController`, `ResultPresentationSettingsController`). Strict `no-store` is enforced on student results, mutations, visits counters, and all admin endpoints via `SensitiveResponseHeadersFilter`. Cloudflare edge caching (Sprint 39) and in-process bounded cache (Sprint 40) build upon this foundation.

> **Sprint 40 Implementation Status:** The public feature snapshot is cached in process for up to 30 seconds with a 128-school LRU cap. Keys use the immutable, gateway-verified school UUID; feature writes evict that school's entry only after commit. Backend feature authorization checks, admin data, sessions, CSRF, results, and mutations are never served from this cache. Set `APP_PUBLIC_FEATURE_CACHE_ENABLED=false` to disable it.

---

## 1. Classification Categories

| Category | Definition | Default Cache-Control | Multi-Tenant Safety Rules |
|---|---|---|---|
| **Public Semi-Static** | Read-only school branding, public principal profile, feature toggles, and academic standard catalogs. Rarely changes (admin edit only). | `public, max-age=300, stale-while-revalidate=60` (proposed in Sprint 38/39) | Cache key **MUST** include edge-verified `schoolId` (e.g. from gateway credential). Never cache across hostnames or tokens. |
| **Public Dynamic** | Notices, study materials, photo gallery albums and images, and official downloads. Updated periodically by school administrators. | `public, max-age=60, stale-while-revalidate=30` (proposed) | Cache key **MUST** include edge-verified `schoolId`. Response must never leak draft/archived items. |
| **Personal Result** | Individual student exam marks and score sheets (`/api/v1/public/exam-results`). Highly sensitive student data. | `no-store, no-cache, must-revalidate, private` (**MANDATORY**) | **NEVER CACHED** anywhere (neither in browser, Cloudflare edge, in-process, nor Redis). Must always bypass cache. |
| **Public Counter** | Live visitor tracking (`/api/v1/public/visits`). | `no-store` | Counters require real-time atomic increment and visibility; never cache. |
| **Authenticated Private** | Admin endpoints (`/api/v1/admin/**`) including dashboard summaries, rosters, audit logs, and settings. | `no-store, no-cache, must-revalidate, private` (**MANDATORY**) | Enforced via `SensitiveResponseHeadersFilter`. Contains session-bound and role-restricted data. |
| **CSRF / Session** | Session establishment, authentication state, login, and CSRF token endpoints (`/api/v1/admin/auth/**`). | `no-store` (**MANDATORY**) | Any response containing `Set-Cookie` or CSRF tokens must **NEVER** be cached. |
| **Mutations** | All `POST`, `PUT`, `PATCH`, and `DELETE` HTTP methods. | `no-store` | Never eligible for caching. Triggers cache invalidation for relevant tenant content. |
| **Unknown / Unclassified**| Any unrecognized path or new endpoint. | `no-store` (**SAFE DEFAULT**) | Closed by default. An endpoint is uncacheable until explicitly added to this matrix. |

---

## 2. Route-by-Route Cache Eligibility Matrix

| HTTP Method | Route Template | Category | Target Cache-Control (Sprint 38+) | Proposed Edge TTL | Tenant Key Source | Invalidation Trigger | Vary Headers | Cookie / Auth Handling |
|---|---|---|---|---|---|---|---|---|
| `GET` | `/api/v1/public/school` | Public Semi-Static | `public, max-age=300, s-maxage=300` | 5 min | Verified Tenant `schoolId` | Admin school update / logo upload | `Accept-Encoding` | Disregards cookies; public |
| `GET` | `/api/v1/public/principal-profile` | Public Semi-Static | `public, max-age=300, s-maxage=300` | 5 min | Verified Tenant `schoolId` | Admin profile update / photo upload | `Accept-Encoding` | Disregards cookies; public |
| `GET` | `/api/v1/public/features` | Public Semi-Static | `public, max-age=180, s-maxage=180` | 3 min | Verified Tenant `schoolId` | Admin feature flag toggle | `Accept-Encoding` | Disregards cookies; public |
| `GET` | `/api/v1/public/academic/standards` | Public Semi-Static | `public, max-age=600, s-maxage=600` | 10 min | Verified Tenant `schoolId` | Admin standard create/delete | `Accept-Encoding` | Disregards cookies; public |
| `GET` | `/api/v1/public/academic/standards/{id}/subjects` | Public Semi-Static | `public, max-age=600, s-maxage=600` | 10 min | Verified Tenant `schoolId` | Admin subject mapping change | `Accept-Encoding` | Disregards cookies; public |
| `GET` | `/api/v1/public/notices` | Public Dynamic | `public, max-age=60, s-maxage=60` | 1 min | Verified Tenant `schoolId` + `?page=&size=` | Notice publish, pin, update, delete | `Accept-Encoding` | Disregards cookies; public |
| `GET` | `/api/v1/public/notices/{id}` | Public Dynamic | `public, max-age=60, s-maxage=60` | Browser/backend only; Worker exact-route allowlist bypasses detail paths | Verified Tenant `schoolId` + notice ID | Notice publish, attachment, update, expiry, delete | `Accept-Encoding` | Disregards cookies; published, unexpired notice only |
| `GET` | `/api/v1/public/materials` | Public Dynamic | `public, max-age=60, s-maxage=60` | 1 min | Verified Tenant `schoolId` + `?page=&size=` | Material publish, update, delete | `Accept-Encoding` | Disregards cookies; public |
| `GET` | `/api/v1/public/gallery/albums` | Public Dynamic | `public, max-age=60, s-maxage=60` | 1 min | Verified Tenant `schoolId` + `?page=&size=` | Album publish, update, delete | `Accept-Encoding` | Disregards cookies; public |
| `GET` | `/api/v1/public/gallery/albums/{id}/images` | Public Dynamic | `public, max-age=60, s-maxage=60` | 1 min | Verified Tenant `schoolId` + albumId + pagination | Image upload, reorder, delete | `Accept-Encoding` | Disregards cookies; public |
| `GET` | `/api/v1/public/downloads` | Public Dynamic | `public, max-age=60, s-maxage=60` | 1 min | Verified Tenant `schoolId` + `?page=&size=` | Download publish, update, delete | `Accept-Encoding` | Disregards cookies; public |
| `GET` | `/api/v1/public/result-settings` | Public Semi-Static | `public, max-age=300, s-maxage=300` | 5 min | Verified Tenant `schoolId` + `?standard=` | Admin result presentation update | `Accept-Encoding` | Disregards cookies; public |
| `GET` | `/api/v1/public/exam-results` | **Personal Result** | `no-store, no-cache, must-revalidate, private` | **0 (NEVER)** | N/A | Immediate direct DB query | None | Strictly uncached; private student records |
| `GET` | `/api/v1/public/visits` | Public Counter | `no-store` | **0 (NEVER)** | N/A | Real-time counter read | None | Strictly uncached |
| `POST` | `/api/v1/public/visits` | Mutation | `no-store` | **0 (NEVER)** | N/A | Increments visit counter | None | Strictly uncached |
| `GET` | `/api/v1/admin/auth/csrf` | CSRF / Session | `no-store` | **0 (NEVER)** | N/A | Dynamic token generation | None | Sets / binds CSRF token; never cached |
| `POST` | `/api/v1/admin/auth/login` | CSRF / Session | `no-store` | **0 (NEVER)** | N/A | Authentication mutation | None | Emits `Set-Cookie` (`JSESSIONID`); never cached |
| `POST` | `/api/v1/admin/auth/logout` | CSRF / Session | `no-store` | **0 (NEVER)** | N/A | Session invalidation | None | Clears `Set-Cookie`; never cached |
| `GET` | `/api/v1/admin/auth/me` | Authenticated Private | `no-store, no-cache, must-revalidate, private` | **0 (NEVER)** | N/A | Dynamic session principal | `Cookie` | Session-dependent; never cached |
| `GET` | `/api/v1/admin/dashboard/**` | Authenticated Private | `no-store, no-cache, must-revalidate, private` | **0 (NEVER)** | N/A | Dynamic aggregation | `Cookie` | Session-dependent; never cached |
| `GET` | `/api/v1/admin/academic/**` | Authenticated Private | `no-store, no-cache, must-revalidate, private` | **0 (NEVER)** | N/A | Admin roster / setup | `Cookie` | Session-dependent; never cached |
| `GET` | `/api/v1/admin/audit-logs` | Authenticated Private | `no-store, no-cache, must-revalidate, private` | **0 (NEVER)** | N/A | Append-only event stream | `Cookie` | Session-dependent; never cached |
| `*` | Any `POST`, `PUT`, `DELETE` | Mutation | `no-store` | **0 (NEVER)** | N/A | State mutation | None | Never cached |
| `*` | Unknown / Unlisted route | Unknown | `no-store` | **0 (NEVER)** | N/A | Closed default | None | Default safe no-store |

---

## 3. Strict Safety Invariants

1. **The `Set-Cookie` Invariant:** Any HTTP response containing a `Set-Cookie` header **MUST NEVER** be cached by any intermediate proxy or CDN (e.g. Cloudflare Worker or edge). Caching a `Set-Cookie` header leads to catastrophic session fixation across users.
2. **The Student Result Invariant:** Student exam results (`/api/v1/public/exam-results`) are classified as `Personal Result` and carry `Cache-Control: no-store`. Responses must never be stored in browser HTTP cache or shared edge caches.
3. **The Tenant Partitioning Invariant:** When public caching is enabled in future sprints (Sprints 38–40), every cache key **MUST** include the verified `schoolId`. Caching based on URL path alone is strictly forbidden because multiple tenant schools share the same path structures on their respective worker portals.
4. **The Admin Boundary Invariant:** All `/api/v1/admin/**` routes are protected by `SensitiveResponseHeadersFilter` which unconditionally emits `Cache-Control: no-store` and `Pragma: no-cache`.
