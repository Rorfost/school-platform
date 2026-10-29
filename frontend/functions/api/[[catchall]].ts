import type { EventContext } from "@cloudflare/workers-types";
import {
    buildCacheKey,
    buildCacheableResponse,
    isCacheEligibleRequest,
    isCacheEligibleResponse,
    matchesCacheableRoute,
    tagCacheHit,
} from "../../worker/cache";

export interface PagesEnv {
    BACKEND_ORIGIN?: string;
    PORTAL_GATEWAY_TOKEN?: string;
    SCHOOL_SLUG?: string;
    API_RATE_LIMITER?: RateLimit;
    LOGIN_RATE_LIMITER?: RateLimit;
    RESULT_RATE_LIMITER?: RateLimit;
    UPLOAD_RATE_LIMITER?: RateLimit;
}

const FORBIDDEN_INCOMING_HEADERS = [
    "forwarded",
    "host",
    "x-forwarded-for",
    "x-forwarded-host",
    "x-forwarded-proto",
    "x-forwarded-server",
];

export async function onRequest(
    context: EventContext<PagesEnv, string, Record<string, unknown>>,
): Promise<Response> {
    const { request, env } = context;
    const url = new URL(request.url);

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

    if (decodedPath.includes("..") || decodedPath.includes("//") || url.pathname.includes("//")) {
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

    const schoolSlug = env.SCHOOL_SLUG ?? "unknown";

    // ── Edge Rate Limiting (Cloudflare Workers / Pages Rate Limiting API) ─
    const cfConnectingIp = request.headers.get("CF-Connecting-IP") || "direct";
    const rateLimitKey = `${schoolSlug}:${cfConnectingIp}`;

    let limiter: RateLimit | undefined;
    const normalizedPath = url.pathname;

    if (normalizedPath === "/api/v1/admin/auth/login") {
        limiter = env.LOGIN_RATE_LIMITER ?? env.API_RATE_LIMITER;
    } else if (normalizedPath.startsWith("/api/v1/public/exam-results")) {
        limiter = env.RESULT_RATE_LIMITER ?? env.API_RATE_LIMITER;
    } else if (
        normalizedPath.includes("/upload") ||
        (normalizedPath.includes("/admin/gallery/albums/") && request.method === "POST")
    ) {
        limiter = env.UPLOAD_RATE_LIMITER ?? env.API_RATE_LIMITER;
    } else {
        limiter = env.API_RATE_LIMITER;
    }

    if (limiter) {
        try {
            const outcome = await limiter.limit({ key: rateLimitKey });
            if (!outcome.success) {
                return new Response(
                    JSON.stringify({
                        status: 429,
                        code: "rate_limit_exceeded",
                        detail: "વિનંતી મર્યાદા વટાવી ગઈ છે. કૃપા કરીને થોડી વાર પછી ફરી પ્રયાસ કરો.",
                    }),
                    {
                        status: 429,
                        headers: {
                            "Content-Type": "application/json;charset=UTF-8",
                            "Retry-After": "60",
                        },
                    },
                );
            }
        } catch {
            // Fail-open for edge rate limiter error; backend rate limiter remains active
        }
    }

    // ── Edge cache check (Public GET Allowlist) ──────────────────────────
    const cacheEligible = isCacheEligibleRequest(request);
    const cacheEntry = cacheEligible ? matchesCacheableRoute(url.pathname) : null;

    if (cacheEntry !== null) {
        try {
            const edgeCache = caches.default;
            const cacheKey = buildCacheKey(schoolSlug, request.url);
            const cachedResponse = await edgeCache.match(cacheKey);
            if (cachedResponse !== undefined) {
                return tagCacheHit(cachedResponse);
            }
        } catch {
            // Cache API unavailable — continue with normal proxy.
        }
    }

    // ── Sanitize headers and inject trusted gateway headers ──────────────
    const requestHeaders = new Headers();
    for (const [key, value] of request.headers.entries()) {
        const lower = key.toLowerCase();
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

    requestHeaders.set("Host", targetUrl.host);

    if (env.PORTAL_GATEWAY_TOKEN) {
        requestHeaders.set("X-Portal-Gateway-Token", env.PORTAL_GATEWAY_TOKEN);
    }

    if (cfConnectingIp && cfConnectingIp !== "direct") {
        requestHeaders.set("X-Portal-Client-IP", cfConnectingIp);
    }

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
        } else {
            const setCookie = backendResponse.headers.get("set-cookie");
            if (setCookie) {
                responseHeaders.set("set-cookie", setCookie);
            }
        }

        // Rewrite redirect location if pointing directly to backend origin
        const location = responseHeaders.get("location");
        if (location) {
            try {
                const locUrl = new URL(location, targetUrl);
                if (locUrl.origin === parsedOrigin.origin) {
                    responseHeaders.set("location", locUrl.pathname + locUrl.search + locUrl.hash);
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

        // Store the response in the edge cache if eligible
        if (cacheEntry !== null && isCacheEligibleResponse(proxiedResponse)) {
            try {
                const edgeCache = caches.default;
                const cacheKey = buildCacheKey(schoolSlug, request.url);
                const responseToCache = buildCacheableResponse(proxiedResponse.clone(), cacheEntry);
                void edgeCache.put(cacheKey, responseToCache);
            } catch {
                // Cache API unavailable or quota exceeded — continue normally.
            }
        }

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
}
