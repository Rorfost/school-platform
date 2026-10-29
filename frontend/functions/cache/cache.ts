/**
 * Edge cache helpers for the Cloudflare Worker gateway proxy.
 *
 * Design constraints (Sprint 39):
 * - Uses the Workers Cache API (`caches.default`), which is available on workers.dev
 *   origins. This is NOT the Cloudflare zone-level CDN cache (which is only available
 *   on custom domains with a zone). Cache hits are per-edge-node, not globally
 *   distributed — but still beneficial for Render cold-start latency reduction.
 * - Cache keys MUST include the verified school slug so that schools sharing identical
 *   URL paths NEVER serve cross-tenant cached responses.
 * - The allowlist is the authoritative source of truth (Sprint 37 matrix). Anything
 *   not on the allowlist is never cached.
 * - Any response that arrives with Set-Cookie, Authorization, no-store, or non-200
 *   status is NEVER cached regardless of the allowlist.
 * - Query parameters that indicate personal/search data bypass caching.
 *
 * Workers Cache API semantics (Wrangler 4 / @cloudflare/workers-types v5):
 * - `caches.default` is the shared cache namespace available to all Workers.
 * - On workers.dev origins, the Cache API operates per-edge-node (not zone-wide CDN).
 * - `cache.put(request|key, response)` stores the response; the key must be a GET Request
 *   or a URL string. Non-GET keys are rejected.
 * - `cache.match(request|key)` returns the cached Response or undefined.
 * - Cached responses respect the `Cache-Control` header's max-age for eviction.
 * - There is NO programmatic purge API in the free Workers Cache; invalidation is TTL-based.
 *
 * References:
 * - https://developers.cloudflare.com/workers/runtime-apis/cache/
 * - Sprint 37 CACHE_POLICY.md eligibility matrix
 * - Sprint 38 implemented Cache-Control headers on backend
 */

/**
 * Routes that are eligible for edge caching.
 * Exact string prefix match against the pathname (without query string).
 */
export interface CacheEntry {
    /** Pathname prefix to match (e.g. "/api/v1/public/school"). */
    pathPrefix: string;
    /**
     * Whether the full path must be an exact match (true) or a prefix match (false).
     * Use exact=true for routes like /api/v1/public/school that must not cache
     * /api/v1/public/school-settings accidentally.
     */
    exact: boolean;
    /** Maximum edge cache TTL in seconds (overrides s-maxage if higher). */
    edgeTtlSeconds: number;
}

/**
 * Allowlisted cacheable public GET routes (Sprint 37 matrix, Public Semi-Static and
 * Public Dynamic categories only). All routes serve published/public data only —
 * the backend already enforces published-state filtering server-side.
 *
 * This is a CLOSED allowlist: any route not present here is NOT cached.
 */
export const CACHEABLE_ROUTES: readonly CacheEntry[] = [
    // Public Semi-Static — 5 min edge TTL
    { pathPrefix: "/api/v1/public/school", exact: true, edgeTtlSeconds: 300 },
    { pathPrefix: "/api/v1/public/principal-profile", exact: true, edgeTtlSeconds: 300 },
    { pathPrefix: "/api/v1/public/features", exact: true, edgeTtlSeconds: 180 },
    { pathPrefix: "/api/v1/public/result-settings", exact: true, edgeTtlSeconds: 300 },
    // Academic standards — prefix match covers /standards and /standards/{id}/subjects
    { pathPrefix: "/api/v1/public/academic/standards", exact: false, edgeTtlSeconds: 600 },
    // Public Dynamic — 1 min edge TTL
    { pathPrefix: "/api/v1/public/notices", exact: true, edgeTtlSeconds: 60 },
    { pathPrefix: "/api/v1/public/materials", exact: true, edgeTtlSeconds: 60 },
    // Gallery albums — prefix match covers /albums and /albums/{id}/images
    { pathPrefix: "/api/v1/public/gallery/albums", exact: false, edgeTtlSeconds: 60 },
    { pathPrefix: "/api/v1/public/downloads", exact: true, edgeTtlSeconds: 60 },
];

/**
 * Query parameters that indicate personal, search, or otherwise non-cacheable
 * requests. If any of these are present, skip caching entirely.
 */
const PERSONAL_QUERY_PARAMS = new Set([
    "studentId",
    "rollNumber",
    "name",
    "q",
    "search",
    "token",
    "session",
]);

/**
 * Determines whether a given request is eligible for edge caching.
 *
 * A request is eligible if:
 * 1. HTTP method is GET
 * 2. No Authorization or Cookie header is present (public endpoints only)
 * 3. URL path matches an entry in CACHEABLE_ROUTES
 * 4. No personal/search query parameters are present
 */
export function isCacheEligibleRequest(request: Request): boolean {
    if (request.method.toUpperCase() !== "GET") {
        return false;
    }

    // Never cache requests carrying session cookies or auth tokens.
    // Public endpoints must not receive session cookies; if they do, bypass.
    if (request.headers.has("Authorization") || request.headers.has("Cookie")) {
        return false;
    }

    const url = new URL(request.url);
    const pathname = url.pathname;

    // Check for personal/search query params
    for (const param of url.searchParams.keys()) {
        if (PERSONAL_QUERY_PARAMS.has(param)) {
            return false;
        }
    }

    return matchesCacheableRoute(pathname) !== null;
}

/**
 * Returns the matching CacheEntry if the pathname matches an allowlisted route,
 * or null if no match.
 */
export function matchesCacheableRoute(pathname: string): CacheEntry | null {
    for (const entry of CACHEABLE_ROUTES) {
        if (entry.exact) {
            if (pathname === entry.pathPrefix) {
                return entry;
            }
        } else {
            // Prefix match: the pathname must equal the prefix OR start with prefix + "/"
            // to avoid matching /api/v1/public/gallery/albums-extra accidentally.
            if (pathname === entry.pathPrefix || pathname.startsWith(entry.pathPrefix + "/")) {
                return entry;
            }
        }
    }
    return null;
}

/**
 * Determines whether a backend response is safe to cache at the edge.
 *
 * A response is NOT cacheable if:
 * 1. Status is not 200
 * 2. Response contains a Set-Cookie header (session fixation invariant)
 * 3. Cache-Control header contains no-store or private
 */
export function isCacheEligibleResponse(response: Response): boolean {
    if (response.status !== 200) {
        return false;
    }

    // Any Set-Cookie header means DO NOT cache (session fixation invariant).
    if (response.headers.has("set-cookie")) {
        return false;
    }

    const cacheControl = (response.headers.get("cache-control") ?? "").toLowerCase();

    // If backend explicitly says no-store or private, honour it.
    if (cacheControl.includes("no-store") || cacheControl.includes("private")) {
        return false;
    }

    return true;
}

/**
 * Builds a stable, per-school cache key URL for use with the Workers Cache API.
 *
 * The Cache API requires a Request or URL string as its key. We construct a
 * synthetic URL that encodes the school slug in the path prefix to ensure complete
 * tenant namespace separation even though all schools share the same Worker code
 * and caches.default namespace.
 *
 * Format: https://cache.internal/{schoolSlug}{pathname}{search}
 *
 * This key is NEVER used for network requests — it is only passed to
 * `cache.put()` and `cache.match()`.
 */
export function buildCacheKey(schoolSlug: string, requestUrl: string): string {
    const url = new URL(requestUrl);
    // Include pathname + search (for pagination params like ?page=1&size=20)
    // but strip fragment (never sent over wire).
    return `https://cache.internal/${encodeURIComponent(schoolSlug)}${url.pathname}${url.search}`;
}

/**
 * Clones a response and sets an explicit max-age based on the route's edge TTL.
 * Honours the backend's own s-maxage if it is lower (more conservative).
 *
 * The cloned response will have:
 * - `Cache-Control: public, max-age=<effective-ttl>, s-maxage=<effective-ttl>, stale-while-revalidate=30`
 * - `X-Cache: MISS` tag for observability (first fetch, will be stored)
 */
export function buildCacheableResponse(response: Response, entry: CacheEntry): Response {
    const headers = new Headers(response.headers);

    // Determine effective TTL: min(backendSMaxAge, edgeTtlSeconds) for conservative caching.
    let effectiveTtl = entry.edgeTtlSeconds;
    const existingCc = headers.get("cache-control") ?? "";
    const sMaxAgeMatch = /s-maxage=(\d+)/.exec(existingCc);
    if (sMaxAgeMatch) {
        const backendSMaxAge = parseInt(sMaxAgeMatch[1], 10);
        effectiveTtl = Math.min(effectiveTtl, backendSMaxAge);
    }

    headers.set(
        "cache-control",
        `public, max-age=${effectiveTtl}, s-maxage=${effectiveTtl}, stale-while-revalidate=30`,
    );
    headers.set("x-cache", "MISS");

    return new Response(response.body, {
        status: response.status,
        statusText: response.statusText,
        headers,
    });
}

/**
 * Clones a cached response and tags it with X-Cache: HIT for observability.
 */
export function tagCacheHit(response: Response): Response {
    const headers = new Headers(response.headers);
    headers.set("x-cache", "HIT");
    return new Response(response.body, {
        status: response.status,
        statusText: response.statusText,
        headers,
    });
}
