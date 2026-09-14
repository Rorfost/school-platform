import { apiBaseUrl } from "@/api/config";

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly requestId?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      Accept: "application/json",
      ...init.headers,
    },
  });

  if (!response.ok) {
    const fallbackMessage = "હમણાં વિનંતી પૂર્ણ થઈ શકી નથી.";
    const payload = (await response.json().catch(() => null)) as { detail?: string } | null;
    throw new ApiError(
      payload?.detail ?? fallbackMessage,
      response.status,
      response.headers.get("X-Request-Id") ?? undefined,
    );
  }

  return (await response.json()) as T;
}
