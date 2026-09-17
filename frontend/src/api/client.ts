import { apiBaseUrl } from "@/api/config";
import type { ProblemDetail } from "@/api/types";
import { getGujaratiErrorMessage } from "@/utils/errors";

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: string,
    readonly requestId?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export function getCsrfTokenFromCookie(): string | null {
  if (typeof document === "undefined") {
    return null;
  }
  const match = document.cookie.match(/(^|;)\s*XSRF-TOKEN\s*=\s*([^;]+)/);
  return match ? decodeURIComponent(match[2]) : null;
}

const defaultCsrfHeaderName = "X-XSRF-TOKEN";
let csrfToken: string | null = null;
let csrfHeaderName = defaultCsrfHeaderName;
let csrfTokenPromise: Promise<string | null> | null = null;

export async function fetchCsrfToken(): Promise<string | null> {
  const existingCookie = getCsrfTokenFromCookie();

  if (existingCookie) {
    return existingCookie;
  }

  if (!csrfTokenPromise) {
    csrfTokenPromise = (async () => {
      try {
        const response = await fetch(`${apiBaseUrl}/api/v1/admin/auth/csrf`, {
          credentials: "include",
          headers: {
            Accept: "application/json",
          },
        });

        if (!response.ok) {
          return null;
        }

        const data = (await response.json()) as { token?: string; headerName?: string };
        csrfHeaderName = data.headerName ?? defaultCsrfHeaderName;
        csrfToken = getCsrfTokenFromCookie() ?? data.token ?? null;
        return csrfToken;
      } catch {
        return null;
      } finally {
        csrfTokenPromise = null;
      }
    })();
  }

  return csrfTokenPromise;
}

export async function refreshCsrfToken(): Promise<string | null> {
  if (!csrfTokenPromise) {
    csrfTokenPromise = (async () => {
      try {
        const response = await fetch(`${apiBaseUrl}/api/v1/admin/auth/csrf`, {
          credentials: "include",
          headers: { Accept: "application/json" },
        });
        if (!response.ok) {
          return null;
        }
        const data = (await response.json()) as { token?: string; headerName?: string };
        csrfHeaderName = data.headerName ?? defaultCsrfHeaderName;
        csrfToken = getCsrfTokenFromCookie() ?? data.token ?? null;
        return csrfToken;
      } catch {
        return null;
      } finally {
        csrfTokenPromise = null;
      }
    })();
  }

  return csrfTokenPromise;
}

export interface ApiRequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { body, headers = {}, method = "GET", ...rest } = options;
  const upperMethod = method.toUpperCase();
  const isMutating = ["POST", "PUT", "PATCH", "DELETE"].includes(upperMethod);

  const requestHeaders = new Headers(headers);
  if (!requestHeaders.has("Accept")) {
    requestHeaders.set("Accept", "application/json");
  }

  if (isMutating) {
    const csrfToken = await fetchCsrfToken();
    if (csrfToken && !requestHeaders.has(csrfHeaderName)) {
      requestHeaders.set(csrfHeaderName, csrfToken);
    }
  }

  let finalBody: BodyInit | undefined;
  if (body !== undefined) {
    if (body instanceof FormData || body instanceof Blob || typeof body === "string") {
      finalBody = body as BodyInit;
    } else {
      requestHeaders.set("Content-Type", "application/json");
      finalBody = JSON.stringify(body);
    }
  }

  let response: Response;
  try {
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...rest,
      method: upperMethod,
      headers: requestHeaders,
      credentials: "include",
      body: finalBody,
    });
  } catch {
    throw new ApiError(getGujaratiErrorMessage("network_error"), 0, "network_error");
  }

  if (!response.ok) {
    let detail: string | undefined;
    let code: string | undefined;

    try {
      const problem = (await response.json()) as ProblemDetail;
      detail = problem.detail;
      code = problem.code;
    } catch {
      // response is not JSON
    }

    const requestId = response.headers.get("X-Request-Id") ?? undefined;
    const message = detail || getGujaratiErrorMessage(code);

    throw new ApiError(message, response.status, code, requestId);
  }

  // Spring returns an empty 200 response for several mutation endpoints. Parsing it as JSON
  // turns a completed delete or publication change into a false client-side failure.
  if (response.status === 204 || response.headers.get("Content-Length") === "0") {
    return undefined as unknown as T;
  }

  const responseText = await response.text();
  if (!responseText.trim()) {
    return undefined as unknown as T;
  }

  return JSON.parse(responseText) as T;
}
