import { describe, expect, it } from "vitest";
import {
    CACHEABLE_ROUTES,
    buildCacheKey,
    buildCacheableResponse,
    isCacheEligibleRequest,
    isCacheEligibleResponse,
    matchesCacheableRoute,
    tagCacheHit,
} from "./cache";

// ---------------------------------------------------------------------------
// Unit tests: pure helper functions for Cloudflare Edge Cache
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
        const req = new Request("https://dhadhana-school-pages.pages.dev/api/v1/public/school");
        expect(isCacheEligibleRequest(req)).toBe(true);
    });

    it("rejects POST requests unconditionally", () => {
        const req = new Request("https://dhadhana-school-pages.pages.dev/api/v1/public/school", {
            method: "POST",
        });
        expect(isCacheEligibleRequest(req)).toBe(false);
    });

    it("rejects requests with Cookie header (session-bearing)", () => {
        const req = new Request("https://dhadhana-school-pages.pages.dev/api/v1/public/school", {
            headers: { Cookie: "JSESSIONID=abc" },
        });
        expect(isCacheEligibleRequest(req)).toBe(false);
    });

    it("rejects requests with Authorization header", () => {
        const req = new Request("https://dhadhana-school-pages.pages.dev/api/v1/public/school", {
            headers: { Authorization: "Bearer token" },
        });
        expect(isCacheEligibleRequest(req)).toBe(false);
    });

    it("rejects requests with personal query param 'studentId'", () => {
        const req = new Request(
            "https://dhadhana-school-pages.pages.dev/api/v1/public/exam-results?studentId=123",
        );
        expect(isCacheEligibleRequest(req)).toBe(false);
    });

    it("rejects requests with personal query param 'rollNumber'", () => {
        const req = new Request(
            "https://dhadhana-school-pages.pages.dev/api/v1/public/notices?rollNumber=12",
        );
        expect(isCacheEligibleRequest(req)).toBe(false);
    });

    it("rejects requests to non-allowlisted routes even if GET", () => {
        const req = new Request(
            "https://dhadhana-school-pages.pages.dev/api/v1/public/exam-results",
        );
        expect(isCacheEligibleRequest(req)).toBe(false);
    });

    it("allows paginated GET requests with safe query params", () => {
        const req = new Request(
            "https://dhadhana-school-pages.pages.dev/api/v1/public/notices?page=2&size=20",
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
        const key = buildCacheKey(
            "dhadhana",
            "https://dhadhana-school-pages.pages.dev/api/v1/public/school",
        );
        expect(key).toContain("dhadhana");
        expect(key).toContain("/api/v1/public/school");
    });

    it("includes query string for pagination isolation", () => {
        const key = buildCacheKey(
            "school-a",
            "https://school-a.pages.dev/api/v1/public/notices?page=2&size=20",
        );
        expect(key).toContain("page=2");
        expect(key).toContain("size=20");
    });

    it("produces DIFFERENT keys for school-a and school-b on the same path", () => {
        const keyA = buildCacheKey("school-a", "https://school-a.pages.dev/api/v1/public/school");
        const keyB = buildCacheKey("school-b", "https://school-b.pages.dev/api/v1/public/school");
        expect(keyA).not.toBe(keyB);
    });

    it("produces SAME key for same school, same path, same params", () => {
        const key1 = buildCacheKey(
            "dhadhana",
            "https://dhadhana-school-pages.pages.dev/api/v1/public/notices?page=1",
        );
        const key2 = buildCacheKey(
            "dhadhana",
            "https://dhadhana-school-pages.pages.dev/api/v1/public/notices?page=1",
        );
        expect(key1).toBe(key2);
    });

    it("produces DIFFERENT keys for same school, same path, different pagination", () => {
        const key1 = buildCacheKey(
            "dhadhana",
            "https://dhadhana-school-pages.pages.dev/api/v1/public/notices?page=1",
        );
        const key2 = buildCacheKey(
            "dhadhana",
            "https://dhadhana-school-pages.pages.dev/api/v1/public/notices?page=2",
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
