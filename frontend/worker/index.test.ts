import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import worker, { type Env } from "./index";

describe("Cloudflare Worker Gateway Proxy", () => {
    let mockAssetsFetch: ReturnType<typeof vi.fn>;
    let mockGlobalFetch: ReturnType<typeof vi.fn>;
    let env: Env;

    beforeEach(() => {
        mockAssetsFetch = vi.fn().mockResolvedValue(new Response("asset content", { status: 200 }));
        mockGlobalFetch = vi.fn();
        vi.stubGlobal("fetch", mockGlobalFetch);

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

    describe("Static Assets Routing", () => {
        it("delegates non-API requests directly to ASSETS.fetch", async () => {
            const request = new Request("https://dhadhana.workers.dev/about");
            const response = await worker.fetch(request, env);

            expect(mockAssetsFetch).toHaveBeenCalledTimes(1);
            expect(mockGlobalFetch).not.toHaveBeenCalled();
            expect(response.status).toBe(200);
            expect(await response.text()).toBe("asset content");
        });

        it("delegates home route / to ASSETS.fetch", async () => {
            const request = new Request("https://dhadhana.workers.dev/");
            await worker.fetch(request, env);

            expect(mockAssetsFetch).toHaveBeenCalledTimes(1);
            expect(mockGlobalFetch).not.toHaveBeenCalled();
        });
    });

    describe("Security & Header Sanitization", () => {
        it("strips client-supplied routing headers and injects secret gateway token", async () => {
            mockGlobalFetch.mockResolvedValueOnce(
                new Response(JSON.stringify({ schoolName: "Dhadhana" }), {
                    status: 200,
                    headers: { "Content-Type": "application/json" },
                }),
            );

            const request = new Request("https://dhadhana.workers.dev/api/v1/public/school", {
                headers: {
                    "X-Portal-Gateway-Token": "attacker-spoofed-token",
                    "X-School-Id": "attacker-school-uuid",
                    "X-Forwarded-For": "203.0.113.195",
                    Forwarded: "for=203.0.113.195",
                    Accept: "application/json",
                    "User-Agent": "TestBrowser/1.0",
                },
            });

            await worker.fetch(request, env);

            expect(mockGlobalFetch).toHaveBeenCalledTimes(1);
            const [targetUrl, proxyInit] = mockGlobalFetch.mock.calls[0];

            expect(targetUrl).toBe("https://school-backend.internal/api/v1/public/school");
            expect(proxyInit.method).toBe("GET");

            const forwardedHeaders = proxyInit.headers as Headers;
            expect(forwardedHeaders.get("X-Portal-Gateway-Token")).toBe(
                "valid-secret-gateway-token-12345",
            );
            expect(forwardedHeaders.get("X-Portal-School-Slug")).toBe("dhadhana");
            expect(forwardedHeaders.get("X-School-Id")).toBeNull();
            expect(forwardedHeaders.get("X-Forwarded-For")).toBeNull();
            expect(forwardedHeaders.get("Forwarded")).toBeNull();
            expect(forwardedHeaders.get("Accept")).toBe("application/json");
            expect(forwardedHeaders.get("User-Agent")).toBe("TestBrowser/1.0");
            expect(forwardedHeaders.get("Host")).toBe("school-backend.internal");
        });

        it("strips internal headers from backend response before sending to client", async () => {
            mockGlobalFetch.mockResolvedValueOnce(
                new Response(JSON.stringify({ ok: true }), {
                    status: 200,
                    headers: {
                        "Content-Type": "application/json",
                        "X-Portal-Debug": "internal-debug-value",
                        "X-School-Secret": "sensitive-data",
                        "X-Custom-Header": "safe-value",
                    },
                }),
            );

            const request = new Request("https://dhadhana.workers.dev/api/v1/public/test");
            const response = await worker.fetch(request, env);

            expect(response.headers.get("X-Custom-Header")).toBe("safe-value");
            expect(response.headers.get("X-Portal-Debug")).toBeNull();
            expect(response.headers.get("X-School-Secret")).toBeNull();
            expect(response.headers.get("X-Portal-Gateway-Token")).toBeNull();
        });

        it("preserves Set-Cookie headers from backend response", async () => {
            const headers = new Headers();
            headers.append("Set-Cookie", "JSESSIONID=session-xyz; Path=/; HttpOnly; Secure");
            headers.append("Set-Cookie", "XSRF-TOKEN=csrf-xyz; Path=/; Secure");

            mockGlobalFetch.mockResolvedValueOnce(
                new Response(JSON.stringify({ authenticated: true }), {
                    status: 200,
                    headers,
                }),
            );

            const request = new Request("https://dhadhana.workers.dev/api/v1/admin/auth/login", {
                method: "POST",
            });
            const response = await worker.fetch(request, env);

            const responseCookies = response.headers.getSetCookie?.() ?? [
                response.headers.get("set-cookie"),
            ];
            expect(responseCookies.some((c) => c?.includes("JSESSIONID=session-xyz"))).toBe(true);
        });
    });

    describe("SSRF and Path Traversal Protection", () => {
        it("rejects path traversal attempts with 400 Bad Request", async () => {
            const encodedTraversalReq = new Request(
                "https://dhadhana.workers.dev/api/%252e%252e/admin/me",
            );
            const encodedRes = await worker.fetch(encodedTraversalReq, env);
            expect(encodedRes.status).toBe(400);

            const body = (await encodedRes.json()) as { code: string };
            expect(body.code).toBe("invalid_path");
            expect(mockGlobalFetch).not.toHaveBeenCalled();
        });

        it("ensures path traversal escaping /api/ resolves to assets and never reaches backend", async () => {
            const request = new Request("https://dhadhana.workers.dev/api/../etc/passwd");
            const response = await worker.fetch(request, env);

            expect(mockAssetsFetch).toHaveBeenCalledTimes(1);
            expect(mockGlobalFetch).not.toHaveBeenCalled();
            expect(response.status).toBe(200);
        });

        it("rejects double-slash path traversal with 400 Bad Request", async () => {
            const request = new Request("https://dhadhana.workers.dev/api//admin/me");
            const response = await worker.fetch(request, env);

            expect(response.status).toBe(400);
            expect(mockGlobalFetch).not.toHaveBeenCalled();
        });

        it("rewrites backend redirect location to relative path", async () => {
            mockGlobalFetch.mockResolvedValueOnce(
                new Response(null, {
                    status: 302,
                    headers: {
                        Location: "https://school-backend.internal/api/v1/auth/login?expired=true",
                    },
                }),
            );

            const request = new Request("https://dhadhana.workers.dev/api/v1/admin/dashboard");
            const response = await worker.fetch(request, env);

            expect(response.status).toBe(302);
            expect(response.headers.get("Location")).toBe("/api/v1/auth/login?expired=true");
        });
    });

    describe("Body Streaming & Query Parameters", () => {
        it("preserves query parameters on public GET requests", async () => {
            mockGlobalFetch.mockResolvedValueOnce(
                new Response(JSON.stringify({ items: [] }), { status: 200 }),
            );

            const request = new Request(
                "https://dhadhana.workers.dev/api/v1/public/notices?page=2&size=15&search=exam",
            );
            await worker.fetch(request, env);

            expect(mockGlobalFetch).toHaveBeenCalledWith(
                "https://school-backend.internal/api/v1/public/notices?page=2&size=15&search=exam",
                expect.objectContaining({
                    method: "GET",
                    redirect: "manual",
                }),
            );
        });

        it("streams request body on POST upload requests", async () => {
            mockGlobalFetch.mockResolvedValueOnce(
                new Response(JSON.stringify({ uploaded: true }), { status: 201 }),
            );

            const payload = JSON.stringify({ academicYearId: "year-1", rollNumber: "12" });
            const request = new Request("https://dhadhana.workers.dev/api/v1/admin/assessments", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: payload,
            });

            await worker.fetch(request, env);

            expect(mockGlobalFetch).toHaveBeenCalledTimes(1);
            const [, proxyInit] = mockGlobalFetch.mock.calls[0];
            expect(proxyInit.method).toBe("POST");
            expect(proxyInit.body).toBeDefined();
        });
    });

    describe("Backend Error Handling", () => {
        it("returns structured 503 JSON when upstream backend is unreachable", async () => {
            mockGlobalFetch.mockRejectedValueOnce(new Error("Network connection refused"));

            const request = new Request("https://dhadhana.workers.dev/api/v1/public/materials");
            const response = await worker.fetch(request, env);

            expect(response.status).toBe(503);
            expect(response.headers.get("Content-Type")).toContain("application/json");

            const body = (await response.json()) as {
                status: number;
                code: string;
                detail: string;
            };
            expect(body.status).toBe(503);
            expect(body.code).toBe("backend_unavailable");
            expect(body.detail).toBe("હમણાં માહિતી મળી શકી નથી. ફરી પ્રયત્ન કરો.");
        });
    });
});
