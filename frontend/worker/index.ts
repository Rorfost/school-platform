export interface Env {
  ASSETS: Fetcher;
  BACKEND_ORIGIN?: string;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    // Proxy API requests to backend origin
    if (url.pathname.startsWith("/api/")) {
      const backendOrigin = (env.BACKEND_ORIGIN || "http://localhost:8080").replace(/\/$/, "");
      const targetUrl = new URL(url.pathname + url.search, backendOrigin);

      const requestHeaders = new Headers(request.headers);
      requestHeaders.set("Host", targetUrl.host);

      const proxyInit: RequestInit = {
        method: request.method,
        headers: requestHeaders,
        redirect: "manual",
      };

      if (
        ["POST", "PUT", "PATCH", "DELETE"].includes(request.method.toUpperCase()) &&
        request.body
      ) {
        proxyInit.body = request.body;
      }

      try {
        const backendResponse = await fetch(targetUrl.toString(), proxyInit);
        const responseHeaders = new Headers(backendResponse.headers);

        return new Response(backendResponse.body, {
          status: backendResponse.status,
          statusText: backendResponse.statusText,
          headers: responseHeaders,
        });
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

    // Serve static frontend assets / SPA routing
    return env.ASSETS.fetch(request);
  },
};
