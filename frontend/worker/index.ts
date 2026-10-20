import {
    buildCacheKey,
    buildCacheableResponse,
    isCacheEligibleRequest,
    isCacheEligibleResponse,
    matchesCacheableRoute,
    tagCacheHit,
} from "./cache";

export interface Env {
    ASSETS: Fetcher;
    BACKEND_ORIGIN?: string;
    PORTAL_GATEWAY_TOKEN?: string;
    SCHOOL_SLUG?: string;
}

const FORBIDDEN_INCOMING_HEADERS = [
    "forwarded",
    "host",
    "x-forwarded-for",
    "x-forwarded-host",
    "x-forwarded-proto",
    "x-forwarded-server",
];

export default {
    async fetch(request: Request, env: Env): Promise<Response> {
        const url = new URL(request.url);

        // Serve static frontend assets / SPA routing for non-API requests
        if (!url.pathname.startsWith("/api/")) {
            return env.ASSETS.fetch(request);
        }

        // Prevent path traversal attacks and URL-encoded bypasses within API routes
        let decodedPath = url.pathname;
        try {
            decodedPath = decodeURIComponent(decodeURIComponent(url.pathname));
        } catch {
            return new Response(
                JSON.stringify({
                    status: 400,
                    code: "invalid_path",
                    detail: "અમાન્ય વિનંતી પાથ.",
                }),
                {
                    status: 400,
                    headers: {
                        "Content-Type": "application/json;charset=UTF-8",
                    },
                },
            );
        }

        if (
            decodedPath.includes("..") ||
            decodedPath.includes("//") ||
            url.pathname.includes("//")
        ) {
            return new Response(
                JSON.stringify({
                    status: 400,
                    code: "invalid_path",
                    detail: "અમાન્ય વિનંતી પાથ.",
                }),
                {
                    status: 400,
                    headers: {
                        "Content-Type": "application/json;charset=UTF-8",
                    },
                },
            );
        }

        const backendOrigin = (env.BACKEND_ORIGIN || "http://localhost:8080").replace(/\/+$/, "");
        const targetUrl = new URL(url.pathname + url.search, backendOrigin);
        const parsedOrigin = new URL(backendOrigin);

        // ── Edge cache check (Task A — Sprint 39) ─────────────────────────────
        // Only public GET endpoints on the Sprint 37 allowlist are eligible.
        // The school slug forms part of the cache key to guarantee per-tenant
        // namespace isolation even though caches.default is shared across all
        // Worker instances that happen to land on the same edge node.
        const schoolSlug = env.SCHOOL_SLUG ?? "unknown";
        const cacheEligible = isCacheEligibleRequest(request);
        const cacheEntry = cacheEligible ? matchesCacheableRoute(url.pathname) : null;

        if (cacheEntry !== null) {
            try {
                const edgeCache = caches.default;
                const cacheKey = buildCacheKey(schoolSlug, request.url);
                const cachedResponse = await edgeCache.match(cacheKey);
                if (cachedResponse !== undefined) {
                    // Cache HIT — return immediately without hitting the backend.
                    return tagCacheHit(cachedResponse);
                }
            } catch {
                // Cache API unavailable (e.g. local wrangler dev without flag) — continue
                // with normal proxy. Never fail the request due to cache issues.
            }
        }
        // ─────────────────────────────────────────────────────────────────────

        // Sanitize and build fresh outbound headers.
        // All client-supplied x-portal-* and x-school-* headers are stripped here;
        // trusted values are injected below AFTER this loop.
        const requestHeaders = new Headers();
        for (const [key, value] of request.headers.entries()) {
            const lower = key.toLowerCase();
            // Strip any client-supplied tenant, routing, forwarded, or client-IP headers.
            // X-Portal-Client-IP is stripped here and re-injected from CF-Connecting-IP below.
            if (
                lower.startsWith("x-portal-") ||
                lower.startsWith("x-school-") ||
                lower.startsWith("x-forwarded-") ||
                lower === "cf-connecting-ip" ||
                FORBIDDEN_INCOMING_HEADERS.includes(lower)
            ) {
                continue;
            }
            requestHeaders.append(key, value);
        }

        // Explicitly set host to target backend
        requestHeaders.set("Host", targetUrl.host);

        // Inject authenticated gateway secret token
        if (env.PORTAL_GATEWAY_TOKEN) {
            requestHeaders.set("X-Portal-Gateway-Token", env.PORTAL_GATEWAY_TOKEN);
        }

        // Inject the Cloudflare edge-attested client IP as a trusted header.
        // CF-Connecting-IP is set by Cloudflare's edge and is NOT controllable by the client.
        // The backend trusts X-Portal-Client-IP ONLY after successful gateway authentication.
        // This value is injected here, after the sanitisation loop has already stripped
        // any client-supplied X-Portal-* headers, so it cannot be forged.
        const cfConnectingIp = request.headers.get("CF-Connecting-IP");
        if (cfConnectingIp) {
            requestHeaders.set("X-Portal-Client-IP", cfConnectingIp);
        }

        // Optional non-authoritative diagnostic slug
        if (env.SCHOOL_SLUG) {
            requestHeaders.set("X-Portal-School-Slug", env.SCHOOL_SLUG);
        }

        const hasBody =
            ["POST", "PUT", "PATCH", "DELETE"].includes(request.method.toUpperCase()) &&
            request.body !== null;

        const proxyInit: RequestInit = {
            method: request.method,
            headers: requestHeaders,
            redirect: "manual",
            ...(hasBody ? { body: request.body } : {}),
        };

        try {
            const backendResponse = await fetch(targetUrl.toString(), proxyInit);

            // Sanitize upstream response headers
            const responseHeaders = new Headers();
            for (const [key, value] of backendResponse.headers.entries()) {
                const lower = key.toLowerCase();
                // Ensure internal token/routing headers never leak to client
                if (lower.startsWith("x-portal-") || lower.startsWith("x-school-")) {
                    continue;
                }
                responseHeaders.append(key, value);
            }

            // Preserve multiple Set-Cookie headers properly
            const getSetCookieFn = (
                backendResponse.headers as unknown as { getSetCookie?: () => string[] }
            ).getSetCookie;
            if (typeof getSetCookieFn === "function") {
                const cookies = getSetCookieFn.call(backendResponse.headers);
                if (cookies && cookies.length > 0) {
                    responseHeaders.delete("set-cookie");
                    for (const cookie of cookies) {
                        responseHeaders.append("set-cookie", cookie);
                    }
                }
            }

            // Rewrite redirect location if pointing directly to backend origin
            const location = responseHeaders.get("location");
            if (location) {
                try {
                    const locUrl = new URL(location, targetUrl);
                    if (locUrl.origin === parsedOrigin.origin) {
                        responseHeaders.set(
                            "location",
                            locUrl.pathname + locUrl.search + locUrl.hash,
                        );
                    }
                } catch {
                    // Ignore malformed redirect URLs
                }
            }

            const proxiedResponse = new Response(backendResponse.body, {
                status: backendResponse.status,
                statusText: backendResponse.statusText,
                headers: responseHeaders,
            });

            // ── Edge cache store (Task A — Sprint 39) ────────────────────────
            // Store the response in the edge cache if:
            // 1. The route is on the allowlist (cacheEntry !== null)
            // 2. The backend response is eligible (200, no Set-Cookie, no no-store)
            // Uses waitUntil-like background store: we clone the response body
            // (consumed by the client) and put a fresh clone in the cache.
            if (cacheEntry !== null && isCacheEligibleResponse(proxiedResponse)) {
                try {
                    const edgeCache = caches.default;
                    const cacheKey = buildCacheKey(schoolSlug, request.url);
                    // buildCacheableResponse clones the response and sets a stable
                    // Cache-Control with the effective TTL (min of backend s-maxage and
                    // route's edgeTtlSeconds). It also tags the response X-Cache: MISS.
                    const responseToCache = buildCacheableResponse(
                        proxiedResponse.clone(),
                        cacheEntry,
                    );
                    // cache.put is fire-and-forget; errors must not block the response.
                    void edgeCache.put(cacheKey, responseToCache);
                } catch {
                    // Cache API unavailable or quota exceeded — continue normally.
                }
            }
            // ─────────────────────────────────────────────────────────────────

            return proxiedResponse;
        } catch {
            return new Response(
                JSON.stringify({
                    status: 503,
                    code: "backend_unavailable",
                    detail: "હમણાં માહિતી મળી શકી નથી. ફરી પ્રયત્ન કરો.",
                }),
                {
                    status: 503,
                    headers: {
                        "Content-Type": "application/json;charset=UTF-8",
                    },
                },
            );
        }
    },
};
