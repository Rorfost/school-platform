import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
    CACHEABLE_ROUTES,
    buildCacheKey,
    buildCacheableResponse,
    isCacheEligibleRequest,
    isCacheEligibleResponse,
    matchesCacheableRoute,
    tagCacheHit,
} from "./cache";
import worker, { type Env } from "./index";

// ---------------------------------------------------------------------------
// Unit tests: pure helper functions
// ---------------------------------------------------------------------------

describe("matchesCacheableRoute", () => {
    it("matches exact public school route", () => {
        expect(matchesCacheableRoute("/api/v1/public/school")).not.toBeNull();
    });

    it("does NOT match /api/v1/public/school-settings (prefix leakage guard)", () => {
        expect(matchesCacheableRoute("/api/v1/public/school-settings")).toBeNull();
    });

    it("matches /api/v1/public/principal-profile exactly", () => {
        expect(matchesCacheableRoute("/api/v1/public/principal-profile")).not.toBeNull();
    });

    it("matches /api/v1/public/features exactly", () => {
        expect(matchesCacheableRoute("/api/v1/public/features")).not.toBeNull();
    });

    it("matches /api/v1/public/academic/standards exactly", () => {
        expect(matchesCacheableRoute("/api/v1/public/academic/standards")).not.toBeNull();
    });

    it("matches /api/v1/public/academic/standards/42/subjects (prefix match)", () => {
        expect(
            matchesCacheableRoute("/api/v1/public/academic/standards/42/subjects"),
        ).not.toBeNull();
    });

    it("matches /api/v1/public/gallery/albums exactly", () => {
        expect(matchesCacheableRoute("/api/v1/public/gallery/albums")).not.toBeNull();
    });

    it("matches /api/v1/public/gallery/albums/123/images (prefix match)", () => {
        expect(matchesCacheableRoute("/api/v1/public/gallery/albums/123/images")).not.toBeNull();
    });

    it("does NOT match /api/v1/public/gallery/albums-extra (prefix leakage guard)", () => {
        expect(matchesCacheableRoute("/api/v1/public/gallery/albums-extra")).toBeNull();
    });

    it("does NOT match /api/v1/public/exam-results (personal result — never cached)", () => {
        expect(matchesCacheableRoute("/api/v1/public/exam-results")).toBeNull();
    });

    it("does NOT match /api/v1/public/visits (counter — never cached)", () => {
        expect(matchesCacheableRoute("/api/v1/public/visits")).toBeNull();
    });

    it("does NOT match /api/v1/admin/auth/login (auth route)", () => {
        expect(matchesCacheableRoute("/api/v1/admin/auth/login")).toBeNull();
    });

    it("does NOT match /api/v1/admin/dashboard (admin route)", () => {
        expect(matchesCacheableRoute("/api/v1/admin/dashboard")).toBeNull();
    });

    it("does NOT match unknown/arbitrary routes", () => {
        expect(matchesCacheableRoute("/api/v1/internal/something")).toBeNull();
    });

    it("returns correct edgeTtlSeconds for semi-static route", () => {
        const entry = matchesCacheableRoute("/api/v1/public/school");
        expect(entry?.edgeTtlSeconds).toBe(300);
    });

    it("returns correct edgeTtlSeconds for dynamic route", () => {
        const entry = matchesCacheableRoute("/api/v1/public/notices");
        expect(entry?.edgeTtlSeconds).toBe(60);
    });
});

describe("isCacheEligibleRequest", () => {
    it("allows eligible GET request to allowlisted route", () => {
        const req = new Request("https://dhadhana.workers.dev/api/v1/public/school");
        expect(isCacheEligibleRequest(req)).toBe(true);
    });

    it("rejects POST requests unconditionally", () => {
        const req = new Request("https://dhadhana.workers.dev/api/v1/public/school", {
            method: "POST",
        });
        expect(isCacheEligibleRequest(req)).toBe(false);
    });

    it("rejects requests with Cookie header (session-bearing)", () => {
        const req = new Request("https://dhadhana.workers.dev/api/v1/public/school", {
            headers: { Cookie: "JSESSIONID=abc" },
        });
        expect(isCacheEligibleRequest(req)).toBe(false);
    });

    it("rejects requests with Authorization header", () => {
        const req = new Request("https://dhadhana.workers.dev/api/v1/public/school", {
            headers: { Authorization: "Bearer token" },
        });
        expect(isCacheEligibleRequest(req)).toBe(false);
    });

    it("rejects requests with personal query param 'studentId'", () => {
        const req = new Request(
            "https://dhadhana.workers.dev/api/v1/public/exam-results?studentId=123",
        );
        expect(isCacheEligibleRequest(req)).toBe(false);
    });

    it("rejects requests with personal query param 'rollNumber'", () => {
        const req = new Request("https://dhadhana.workers.dev/api/v1/public/notices?rollNumber=12");
        expect(isCacheEligibleRequest(req)).toBe(false);
    });

    it("rejects requests to non-allowlisted routes even if GET", () => {
        const req = new Request("https://dhadhana.workers.dev/api/v1/public/exam-results");
        expect(isCacheEligibleRequest(req)).toBe(false);
    });

    it("allows paginated GET requests with safe query params", () => {
        const req = new Request(
            "https://dhadhana.workers.dev/api/v1/public/notices?page=2&size=20",
        );
        expect(isCacheEligibleRequest(req)).toBe(true);
    });
});

describe("isCacheEligibleResponse", () => {
    it("allows a 200 public response", () => {
        const res = new Response("ok", {
            status: 200,
            headers: { "cache-control": "public, max-age=300" },
        });
        expect(isCacheEligibleResponse(res)).toBe(true);
    });

    it("rejects response with Set-Cookie (session fixation invariant)", () => {
        const headers = new Headers();
        headers.set("set-cookie", "JSESSIONID=abc; HttpOnly; Secure");
        const res = new Response("ok", { status: 200, headers });
        expect(isCacheEligibleResponse(res)).toBe(false);
    });

    it("rejects response with Cache-Control: no-store", () => {
        const res = new Response("private", {
            status: 200,
            headers: { "cache-control": "no-store, no-cache, must-revalidate, private" },
        });
        expect(isCacheEligibleResponse(res)).toBe(false);
    });

    it("rejects response with Cache-Control: private", () => {
        const res = new Response("private", {
            status: 200,
            headers: { "cache-control": "private" },
        });
        expect(isCacheEligibleResponse(res)).toBe(false);
    });

    it("rejects non-200 status (e.g. 404)", () => {
        const res = new Response("not found", { status: 404 });
        expect(isCacheEligibleResponse(res)).toBe(false);
    });

    it("rejects non-200 status (e.g. 500)", () => {
        const res = new Response("error", { status: 500 });
        expect(isCacheEligibleResponse(res)).toBe(false);
    });
});

describe("buildCacheKey", () => {
    it("includes school slug and pathname", () => {
        const key = buildCacheKey("dhadhana", "https://dhadhana.workers.dev/api/v1/public/school");
        expect(key).toContain("dhadhana");
        expect(key).toContain("/api/v1/public/school");
    });

    it("includes query string for pagination isolation", () => {
        const key = buildCacheKey(
            "school-a",
            "https://school-a.workers.dev/api/v1/public/notices?page=2&size=20",
        );
        expect(key).toContain("page=2");
        expect(key).toContain("size=20");
    });

    it("produces DIFFERENT keys for school-a and school-b on the same path", () => {
        const keyA = buildCacheKey("school-a", "https://school-a.workers.dev/api/v1/public/school");
        const keyB = buildCacheKey("school-b", "https://school-b.workers.dev/api/v1/public/school");
        expect(keyA).not.toBe(keyB);
    });

    it("produces SAME key for same school, same path, same params", () => {
        const key1 = buildCacheKey(
            "dhadhana",
            "https://dhadhana.workers.dev/api/v1/public/notices?page=1",
        );
        const key2 = buildCacheKey(
            "dhadhana",
            "https://dhadhana.workers.dev/api/v1/public/notices?page=1",
        );
        expect(key1).toBe(key2);
    });

    it("produces DIFFERENT keys for same school, same path, different pagination", () => {
        const key1 = buildCacheKey(
            "dhadhana",
            "https://dhadhana.workers.dev/api/v1/public/notices?page=1",
        );
        const key2 = buildCacheKey(
            "dhadhana",
            "https://dhadhana.workers.dev/api/v1/public/notices?page=2",
        );
        expect(key1).not.toBe(key2);
    });
});

describe("buildCacheableResponse", () => {
    it("sets Cache-Control with edgeTtlSeconds when no backend s-maxage", () => {
        const entry = CACHEABLE_ROUTES.find((e) => e.pathPrefix === "/api/v1/public/school")!;
        const res = new Response("data", {
            status: 200,
            headers: { "cache-control": "public, max-age=300" },
        });
        const cacheableRes = buildCacheableResponse(res, entry);
        expect(cacheableRes.headers.get("cache-control")).toContain("s-maxage=300");
    });

    it("respects lower backend s-maxage over edgeTtlSeconds", () => {
        const entry = CACHEABLE_ROUTES.find((e) => e.pathPrefix === "/api/v1/public/school")!;
        // Backend says s-maxage=120, route says 300 — use min(120, 300) = 120
        const res = new Response("data", {
            status: 200,
            headers: { "cache-control": "public, max-age=120, s-maxage=120" },
        });
        const cacheableRes = buildCacheableResponse(res, entry);
        expect(cacheableRes.headers.get("cache-control")).toContain("s-maxage=120");
        expect(cacheableRes.headers.get("cache-control")).not.toContain("s-maxage=300");
    });

    it("sets X-Cache: MISS on the response", () => {
        const entry = CACHEABLE_ROUTES.find((e) => e.pathPrefix === "/api/v1/public/school")!;
        const res = new Response("data", { status: 200 });
        const cacheableRes = buildCacheableResponse(res, entry);
        expect(cacheableRes.headers.get("x-cache")).toBe("MISS");
    });

    it("includes stale-while-revalidate", () => {
        const entry = CACHEABLE_ROUTES.find((e) => e.pathPrefix === "/api/v1/public/school")!;
        const res = new Response("data", { status: 200 });
        const cacheableRes = buildCacheableResponse(res, entry);
        expect(cacheableRes.headers.get("cache-control")).toContain("stale-while-revalidate=30");
    });
});

describe("tagCacheHit", () => {
    it("sets X-Cache: HIT", () => {
        const res = new Response("data", { status: 200, headers: { "x-cache": "MISS" } });
        const hitRes = tagCacheHit(res);
        expect(hitRes.headers.get("x-cache")).toBe("HIT");
    });

    it("preserves other response headers", () => {
        const res = new Response("data", {
            status: 200,
            headers: { "content-type": "application/json", etag: '"abc123"' },
        });
        const hitRes = tagCacheHit(res);
        expect(hitRes.headers.get("content-type")).toBe("application/json");
        expect(hitRes.headers.get("etag")).toBe('"abc123"');
    });
});

// ---------------------------------------------------------------------------
// Integration tests: Worker with mock Cache API
// ---------------------------------------------------------------------------

describe("Worker edge cache integration", () => {
    let mockAssetsFetch: ReturnType<typeof vi.fn>;
    let mockGlobalFetch: ReturnType<typeof vi.fn>;
    let mockCacheMatch: ReturnType<typeof vi.fn>;
    let mockCachePut: ReturnType<typeof vi.fn>;
    let env: Env;

    beforeEach(() => {
        mockAssetsFetch = vi.fn().mockResolvedValue(new Response("asset", { status: 200 }));
        mockGlobalFetch = vi.fn();
        mockCacheMatch = vi.fn();
        mockCachePut = vi.fn().mockResolvedValue(undefined);

        vi.stubGlobal("fetch", mockGlobalFetch);
        vi.stubGlobal("caches", {
            default: {
                match: mockCacheMatch,
                put: mockCachePut,
            },
        });

        env = {
            ASSETS: { fetch: mockAssetsFetch } as unknown as Fetcher,
            BACKEND_ORIGIN: "https://school-backend.internal",
            PORTAL_GATEWAY_TOKEN: "valid-secret-gateway-token-12345",
            SCHOOL_SLUG: "dhadhana",
        };
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.restoreAllMocks();
    });

    it("returns cached response on cache HIT without hitting backend", async () => {
        const cachedBody = JSON.stringify({ schoolName: "Dhadhana Primary" });
        mockCacheMatch.mockResolvedValueOnce(
            new Response(cachedBody, {
                status: 200,
                headers: {
                    "content-type": "application/json",
                    "cache-control": "public, max-age=300, s-maxage=300",
                    "x-cache": "MISS",
                },
            }),
        );

        const request = new Request("https://dhadhana.workers.dev/api/v1/public/school");
        const response = await worker.fetch(request, env);

        // Backend was NOT called
        expect(mockGlobalFetch).not.toHaveBeenCalled();
        // Response is tagged HIT
        expect(response.headers.get("x-cache")).toBe("HIT");
        expect(response.status).toBe(200);
    });

    it("fetches from backend and stores in cache on cache MISS", async () => {
        // Cache miss
        mockCacheMatch.mockResolvedValueOnce(undefined);
        // Backend returns cacheable response
        mockGlobalFetch.mockResolvedValueOnce(
            new Response(JSON.stringify({ schoolName: "Dhadhana" }), {
                status: 200,
                headers: {
                    "content-type": "application/json",
                    "cache-control": "public, max-age=300, s-maxage=300",
                },
            }),
        );

        const request = new Request("https://dhadhana.workers.dev/api/v1/public/school");
        const response = await worker.fetch(request, env);

        expect(mockGlobalFetch).toHaveBeenCalledTimes(1);
        // Cache put was called (fire-and-forget)
        await vi.waitFor(() => expect(mockCachePut).toHaveBeenCalledTimes(1));
        expect(response.status).toBe(200);
    });

    it("does NOT cache a response that carries Set-Cookie", async () => {
        mockCacheMatch.mockResolvedValueOnce(undefined);

        const responseHeaders = new Headers();
        responseHeaders.set("content-type", "application/json");
        responseHeaders.set("cache-control", "public, max-age=300");
        responseHeaders.append("set-cookie", "JSESSIONID=abc; HttpOnly; Secure; SameSite=Lax");

        mockGlobalFetch.mockResolvedValueOnce(
            new Response(JSON.stringify({ schoolName: "X" }), {
                status: 200,
                headers: responseHeaders,
            }),
        );

        const request = new Request("https://dhadhana.workers.dev/api/v1/public/school");
        await worker.fetch(request, env);

        // Gave a small wait to confirm cache.put is not called asynchronously
        await new Promise((r) => setTimeout(r, 10));
        expect(mockCachePut).not.toHaveBeenCalled();
    });

    it("does NOT cache exam-results (personal data — never on allowlist)", async () => {
        mockGlobalFetch.mockResolvedValueOnce(
            new Response(JSON.stringify({ marks: [] }), {
                status: 200,
                headers: {
                    "content-type": "application/json",
                    "cache-control": "no-store, no-cache, must-revalidate, private",
                },
            }),
        );

        const request = new Request(
            "https://dhadhana.workers.dev/api/v1/public/exam-results?rollNumber=12",
        );
        await worker.fetch(request, env);

        expect(mockCacheMatch).not.toHaveBeenCalled();
        expect(mockCachePut).not.toHaveBeenCalled();
    });

    it("does NOT cache admin routes", async () => {
        mockGlobalFetch.mockResolvedValueOnce(
            new Response(JSON.stringify({ email: "admin@school.org" }), {
                status: 200,
                headers: {
                    "content-type": "application/json",
                    "cache-control": "no-store, no-cache, must-revalidate, private",
                },
            }),
        );

        const request = new Request("https://dhadhana.workers.dev/api/v1/admin/auth/me", {
            headers: { Cookie: "JSESSIONID=abc" },
        });
        await worker.fetch(request, env);

        expect(mockCacheMatch).not.toHaveBeenCalled();
        expect(mockCachePut).not.toHaveBeenCalled();
    });

    it("does NOT cache visit counter route", async () => {
        mockGlobalFetch.mockResolvedValueOnce(
            new Response(JSON.stringify({ count: 1234 }), {
                status: 200,
                headers: { "cache-control": "no-store" },
            }),
        );

        const request = new Request("https://dhadhana.workers.dev/api/v1/public/visits");
        await worker.fetch(request, env);

        expect(mockCacheMatch).not.toHaveBeenCalled();
        expect(mockCachePut).not.toHaveBeenCalled();
    });

    it("uses DIFFERENT cache keys for school-a and school-b on the same path", async () => {
        const makeSchoolResponse = () =>
            new Response(JSON.stringify({ schoolName: "School" }), {
                status: 200,
                headers: {
                    "content-type": "application/json",
                    "cache-control": "public, max-age=300, s-maxage=300",
                },
            });

        // Both return cache miss → backend call; use mockOnce to give fresh bodies each time
        mockCacheMatch.mockResolvedValueOnce(undefined).mockResolvedValueOnce(undefined);
        mockGlobalFetch
            .mockResolvedValueOnce(makeSchoolResponse())
            .mockResolvedValueOnce(makeSchoolResponse());

        const envA: Env = { ...env, SCHOOL_SLUG: "school-a" };
        const envB: Env = { ...env, SCHOOL_SLUG: "school-b" };

        const reqA = new Request("https://school-a.workers.dev/api/v1/public/school");
        const reqB = new Request("https://school-b.workers.dev/api/v1/public/school");

        // Execute sequentially to avoid any race between the void cache.put calls
        await worker.fetch(reqA, envA);
        // Small settle to allow the fire-and-forget put to be queued
        await new Promise((r) => setTimeout(r, 20));

        await worker.fetch(reqB, envB);
        await new Promise((r) => setTimeout(r, 20));

        // Both calls to cache.put should have used distinct keys
        expect(mockCachePut).toHaveBeenCalledTimes(2);

        const [keyA] = mockCachePut.mock.calls[0] as [string, Response];
        const [keyB] = mockCachePut.mock.calls[1] as [string, Response];
        expect(keyA).not.toBe(keyB);
        expect(keyA).toContain("school-a");
        expect(keyB).toContain("school-b");
    });

    it("continues to proxy normally when Cache API throws (graceful degradation)", async () => {
        mockCacheMatch.mockRejectedValueOnce(new Error("Cache API unavailable"));
        mockGlobalFetch.mockResolvedValueOnce(
            new Response(JSON.stringify({ schoolName: "Dhadhana" }), {
                status: 200,
                headers: {
                    "content-type": "application/json",
                    "cache-control": "public, max-age=300",
                },
            }),
        );

        const request = new Request("https://dhadhana.workers.dev/api/v1/public/school");
        const response = await worker.fetch(request, env);

        // Backend was called despite cache error
        expect(mockGlobalFetch).toHaveBeenCalledTimes(1);
        expect(response.status).toBe(200);
    });

    it("does NOT cache non-200 backend responses (e.g. 404)", async () => {
        mockCacheMatch.mockResolvedValueOnce(undefined);
        mockGlobalFetch.mockResolvedValueOnce(
            new Response(JSON.stringify({ error: "not found" }), {
                status: 404,
                headers: { "content-type": "application/json" },
            }),
        );

        const request = new Request("https://dhadhana.workers.dev/api/v1/public/school");
        await worker.fetch(request, env);

        await new Promise((r) => setTimeout(r, 10));
        expect(mockCachePut).not.toHaveBeenCalled();
    });

    it("does NOT cache POST requests", async () => {
        mockGlobalFetch.mockResolvedValueOnce(
            new Response(JSON.stringify({ ok: true }), { status: 200 }),
        );

        const request = new Request("https://dhadhana.workers.dev/api/v1/public/visits", {
            method: "POST",
        });
        await worker.fetch(request, env);

        expect(mockCacheMatch).not.toHaveBeenCalled();
        expect(mockCachePut).not.toHaveBeenCalled();
    });
});
