import type { EventContext } from "@cloudflare/workers-types";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { onRequest, type PagesEnv } from "./[[catchall]]";

describe("Cloudflare Pages Functions API Gateway Proxy", () => {
    let mockGlobalFetch: ReturnType<typeof vi.fn>;
    let env: PagesEnv;

    beforeEach(() => {
        mockGlobalFetch = vi.fn();
        vi.stubGlobal("fetch", mockGlobalFetch);

        env = {
            BACKEND_ORIGIN: "https://school-backend.internal",
            PORTAL_GATEWAY_TOKEN: "valid-secret-gateway-token-12345",
            SCHOOL_SLUG: "dhadhana",
        };
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.restoreAllMocks();
    });

    it("fails closed with 500 when PORTAL_GATEWAY_TOKEN is missing or empty", async () => {
        const envMissingToken: PagesEnv = {
            BACKEND_ORIGIN: "https://school-backend.internal",
            SCHOOL_SLUG: "dhadhana",
        };

        const request = new Request("https://dhadhana.pages.dev/api/v1/public/school");
        const context: EventContext<PagesEnv, string, Record<string, unknown>> = {
            request,
            env: envMissingToken,
            params: {},
            data: {},
            waitUntil: vi.fn(),
            next: vi.fn(),
            functionPath: "/api",
        };

        const response = await onRequest(context);
        expect(response.status).toBe(500);
        const body = (await response.json()) as { code: string };
        expect(body.code).toBe("gateway_configuration_error");
        expect(mockGlobalFetch).not.toHaveBeenCalled();
    });

    it("injects gateway token and trusted client IP from CF-Connecting-IP", async () => {
        mockGlobalFetch.mockResolvedValueOnce(
            new Response(JSON.stringify({ ok: true }), { status: 200 }),
        );

        const request = new Request("https://dhadhana.pages.dev/api/v1/public/school", {
            headers: {
                "CF-Connecting-IP": "203.0.113.10",
                "X-Portal-Client-IP": "spoofed-ip", // Must be stripped
            },
        });

        const context: EventContext<PagesEnv, string, Record<string, unknown>> = {
            request,
            env,
            params: { catchall: ["v1", "public", "school"] },
            data: {},
            waitUntil: vi.fn(),
            next: vi.fn(),
            functionPath: "/api",
        };

        const response = await onRequest(context);
        expect(response.status).toBe(200);

        const [, proxyInit] = mockGlobalFetch.mock.calls[0];
        const headers = proxyInit.headers as Headers;
        expect(headers.get("X-Portal-Gateway-Token")).toBe("valid-secret-gateway-token-12345");
        expect(headers.get("X-Portal-Client-IP")).toBe("203.0.113.10");
        expect(headers.get("X-Portal-School-Slug")).toBe("dhadhana");
    });

    it("enforces edge rate limiting returning 429 when bucket exhausted", async () => {
        const mockLimiter = {
            limit: vi.fn().mockResolvedValue({ success: false }),
        };
        const envWithLimiter: PagesEnv = {
            ...env,
            API_RATE_LIMITER: mockLimiter as unknown as RateLimit,
        };

        const request = new Request("https://dhadhana.pages.dev/api/v1/public/notices", {
            headers: { "CF-Connecting-IP": "198.51.100.5" },
        });

        const context: EventContext<PagesEnv, string, Record<string, unknown>> = {
            request,
            env: envWithLimiter,
            params: {},
            data: {},
            waitUntil: vi.fn(),
            next: vi.fn(),
            functionPath: "/api",
        };

        const response = await onRequest(context);
        expect(response.status).toBe(429);
        expect(response.headers.get("Retry-After")).toBe("60");
        const body = (await response.json()) as { code: string };
        expect(body.code).toBe("rate_limit_exceeded");
        expect(mockGlobalFetch).not.toHaveBeenCalled();
    });

    it("returns structured 503 JSON when upstream backend is unreachable", async () => {
        mockGlobalFetch.mockRejectedValueOnce(new Error("Connection refused"));

        const request = new Request("https://dhadhana.pages.dev/api/v1/public/materials");
        const context: EventContext<PagesEnv, string, Record<string, unknown>> = {
            request,
            env,
            params: {},
            data: {},
            waitUntil: vi.fn(),
            next: vi.fn(),
            functionPath: "/api",
        };

        const response = await onRequest(context);
        expect(response.status).toBe(503);
        const body = (await response.json()) as { code: string };
        expect(body.code).toBe("backend_unavailable");
    });
});
