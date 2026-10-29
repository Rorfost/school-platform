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

    describe("Client IP Injection", () => {
        it("injects CF-Connecting-IP as X-Portal-Client-IP after stripping client-supplied value", async () => {
            mockGlobalFetch.mockResolvedValueOnce(
                new Response(JSON.stringify({ ok: true }), { status: 200 }),
            );

            // Attacker tries to spoof both CF-Connecting-IP and X-Portal-Client-IP
            const request = new Request("https://dhadhana.workers.dev/api/v1/admin/auth/login", {
                method: "POST",
                headers: {
                    "CF-Connecting-IP": "192.0.2.1", // edge-provided (trusted)
                    "X-Portal-Client-IP": "10.0.0.1", // attacker-supplied (must be stripped)
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ email: "a@b.com", password: "p" }),
            });

            await worker.fetch(request, env);

            const [, proxyInit] = mockGlobalFetch.mock.calls[0];
            const headers = proxyInit.headers as Headers;
            // The Worker reads CF-Connecting-IP from its own edge context (here: request header
            // in unit-test), strips the client-supplied X-Portal-Client-IP, and re-injects.
            expect(headers.get("X-Portal-Client-IP")).toBe("192.0.2.1");
        });

        it("does not inject X-Portal-Client-IP when CF-Connecting-IP is absent (non-CF origin)", async () => {
            mockGlobalFetch.mockResolvedValueOnce(
                new Response(JSON.stringify({ ok: true }), { status: 200 }),
            );

            const request = new Request("https://dhadhana.workers.dev/api/v1/admin/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: "a@b.com", password: "p" }),
            });

            await worker.fetch(request, env);

            const [, proxyInit] = mockGlobalFetch.mock.calls[0];
            const headers = proxyInit.headers as Headers;
            expect(headers.get("X-Portal-Client-IP")).toBeNull();
        });

        it("strips client-supplied CF-Connecting-IP to prevent forgery", async () => {
            mockGlobalFetch.mockResolvedValueOnce(
                new Response(JSON.stringify({ ok: true }), { status: 200 }),
            );

            // No real CF-Connecting-IP from the edge — only a client-forged one
            const request = new Request("https://dhadhana.workers.dev/api/v1/public/exam-results", {
                headers: {
                    "CF-Connecting-IP": "1.2.3.4",
                },
            });

            // Remove CF-Connecting-IP from env simulation (non-CF local test)
            // and confirm the header does not bleed through raw
            await worker.fetch(request, env);

            const [, proxyInit] = mockGlobalFetch.mock.calls[0];
            const headers = proxyInit.headers as Headers;
            // In the Worker runtime CF-Connecting-IP comes from the edge, not the client.
            // In this unit-test the Request object carries it like any other header, so the
            // Worker correctly reads it and forwards it as X-Portal-Client-IP (intended behaviour).
            // The critical invariant is: the raw CF-Connecting-IP header must NOT be forwarded.
            expect(headers.get("CF-Connecting-IP")).toBeNull();
            // And x-portal-client-ip is set from the cf value (showing re-injection path works)
            expect(headers.get("X-Portal-Client-IP")).toBe("1.2.3.4");
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
            headers.append(
                "Set-Cookie",
                "JSESSIONID=session-xyz; Path=/; HttpOnly; Secure; SameSite=Lax",
            );
            headers.append("Set-Cookie", "XSRF-TOKEN=csrf-xyz; Path=/; Secure; SameSite=Lax");

            mockGlobalFetch.mockResolvedValueOnce(
                new Response(JSON.stringify({ authenticated: true }), {
                    status: 200,
                    headers,
                }),
            );

            const request = new Request("https://school-a.workers.dev/api/v1/admin/auth/login", {
                method: "POST",
            });
            const response = await worker.fetch(request, env);

            const responseCookies = response.headers.getSetCookie?.() ?? [
                response.headers.get("set-cookie"),
            ];
            expect(responseCookies.some((c) => c?.includes("JSESSIONID=session-xyz"))).toBe(true);
            expect(responseCookies.some((c) => c?.includes("XSRF-TOKEN=csrf-xyz"))).toBe(true);
            // Verify host-only cookie scope: does not specify broad Domain=.workers.dev
            expect(responseCookies.every((c) => !c?.toLowerCase().includes("domain="))).toBe(true);
        });

        it("forwards incoming request session and CSRF cookies to upstream backend", async () => {
            mockGlobalFetch.mockResolvedValueOnce(
                new Response(JSON.stringify({ email: "principal@school-a.org" }), {
                    status: 200,
                    headers: { "Content-Type": "application/json" },
                }),
            );

            const request = new Request("https://school-a.workers.dev/api/v1/admin/auth/me", {
                method: "GET",
                headers: {
                    Cookie: "JSESSIONID=session-a-secret; XSRF-TOKEN=csrf-token-val",
                    "X-XSRF-TOKEN": "csrf-token-val",
                },
            });
            await worker.fetch(request, env);

            expect(mockGlobalFetch).toHaveBeenCalledTimes(1);
            const [, proxyInit] = mockGlobalFetch.mock.calls[0];
            const forwardedHeaders = proxyInit.headers as Headers;
            expect(forwardedHeaders.get("Cookie")).toBe(
                "JSESSIONID=session-a-secret; XSRF-TOKEN=csrf-token-val",
            );
            expect(forwardedHeaders.get("X-XSRF-TOKEN")).toBe("csrf-token-val");
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

        it("streams multipart/form-data request body on exam results Excel upload", async () => {
            mockGlobalFetch.mockResolvedValueOnce(
                new Response(JSON.stringify({ message: "Exam results uploaded successfully" }), {
                    status: 200,
                    headers: { "Content-Type": "application/json" },
                }),
            );

            const multipartBody =
                "--WebKitFormBoundary7MA4YWxkTrZu0gW\r\n" +
                'Content-Disposition: form-data; name="academicYearId"\r\n\r\n' +
                "year-2026-27\r\n" +
                "--WebKitFormBoundary7MA4YWxkTrZu0gW\r\n" +
                'Content-Disposition: form-data; name="file"; filename="results.xlsx"\r\n' +
                "Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n" +
                "binary content\r\n" +
                "--WebKitFormBoundary7MA4YWxkTrZu0gW--\r\n";

            const request = new Request(
                "https://school-a-portal.workers.dev/api/v1/admin/exam-results/upload",
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "multipart/form-data; boundary=WebKitFormBoundary7MA4YWxkTrZu0gW",
                    },
                    body: multipartBody,
                },
            );

            const response = await worker.fetch(request, env);

            expect(mockGlobalFetch).toHaveBeenCalledTimes(1);
            const [targetUrl, proxyInit] = mockGlobalFetch.mock.calls[0];
            expect(targetUrl).toBe(
                "https://school-backend.internal/api/v1/admin/exam-results/upload",
            );
            expect(proxyInit.method).toBe("POST");
            expect(proxyInit.body).toBeDefined();
            expect(response.status).toBe(200);
            const resBody = (await response.json()) as { message: string };
            expect(resBody.message).toBe("Exam results uploaded successfully");
        });
    });

    describe("Environment Isolation", () => {
        it("forwards distinct school slug and gateway token for distinct environment configurations", async () => {
            mockGlobalFetch.mockResolvedValue(
                new Response(JSON.stringify({ ok: true }), { status: 200 }),
            );

            const envSchoolB: Env = {
                ASSETS: { fetch: mockAssetsFetch } as unknown as Fetcher,
                BACKEND_ORIGIN: "https://school-platform-gxrt.onrender.com",
                PORTAL_GATEWAY_TOKEN: "school-b-distinct-secret-token",
                SCHOOL_SLUG: "school-b",
            };

            const requestB = new Request(
                "https://school-b-portal.workers.dev/api/v1/public/school",
            );
            await worker.fetch(requestB, envSchoolB);

            expect(mockGlobalFetch).toHaveBeenCalledTimes(1);
            const [, proxyInitB] = mockGlobalFetch.mock.calls[0];
            const headersB = proxyInitB.headers as Headers;
            expect(headersB.get("X-Portal-School-Slug")).toBe("school-b");
            expect(headersB.get("X-Portal-Gateway-Token")).toBe("school-b-distinct-secret-token");
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

    describe("Edge Rate Limiting", () => {
        it("returns 429 when rate limiter returns success: false", async () => {
            const mockRateLimiter = {
                limit: vi.fn().mockResolvedValue({ success: false }),
            };
            const envWithLimiter: Env = {
                ...env,
                API_RATE_LIMITER: mockRateLimiter as unknown as RateLimit,
            };

            const request = new Request("https://dhadhana.workers.dev/api/v1/public/notices", {
                headers: { "CF-Connecting-IP": "203.0.113.1" },
            });
            const response = await worker.fetch(request, envWithLimiter);

            expect(response.status).toBe(429);
            expect(response.headers.get("Retry-After")).toBe("60");
            const body = (await response.json()) as { code: string };
            expect(body.code).toBe("rate_limit_exceeded");
            expect(mockRateLimiter.limit).toHaveBeenCalledWith({ key: "dhadhana:203.0.113.1" });
            expect(mockGlobalFetch).not.toHaveBeenCalled();
        });

        it("routes login requests to LOGIN_RATE_LIMITER", async () => {
            const mockLoginLimiter = {
                limit: vi.fn().mockResolvedValue({ success: true }),
            };
            const envWithLimiter: Env = {
                ...env,
                LOGIN_RATE_LIMITER: mockLoginLimiter as unknown as RateLimit,
            };

            mockGlobalFetch.mockResolvedValueOnce(
                new Response(JSON.stringify({ ok: true }), { status: 200 }),
            );

            const request = new Request("https://dhadhana.workers.dev/api/v1/admin/auth/login", {
                method: "POST",
                headers: { "CF-Connecting-IP": "203.0.113.2" },
            });
            await worker.fetch(request, envWithLimiter);

            expect(mockLoginLimiter.limit).toHaveBeenCalledWith({ key: "dhadhana:203.0.113.2" });
        });

        it("does not rate limit static asset requests", async () => {
            const mockRateLimiter = {
                limit: vi.fn().mockResolvedValue({ success: false }),
            };
            const envWithLimiter: Env = {
                ...env,
                API_RATE_LIMITER: mockRateLimiter as unknown as RateLimit,
            };

            const request = new Request("https://dhadhana.workers.dev/assets/index.js");
            const response = await worker.fetch(request, envWithLimiter);

            expect(response.status).toBe(200);
            expect(mockRateLimiter.limit).not.toHaveBeenCalled();
            expect(mockAssetsFetch).toHaveBeenCalledTimes(1);
        });
    });
});
