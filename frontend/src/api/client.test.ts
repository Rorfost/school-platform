import { apiRequest, getCsrfTokenFromCookie, refreshCsrfToken } from "@/api/client";
import { getGujaratiErrorMessage } from "@/utils/errors";

describe("api client & error utilities", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
  });

  it("extracts CSRF token from document cookie", () => {
    document.cookie = "XSRF-TOKEN=test-token-value; Path=/";
    expect(getCsrfTokenFromCookie()).toBe("test-token-value");
  });

  it("returns null when XSRF-TOKEN cookie is not present", () => {
    document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
    expect(getCsrfTokenFromCookie()).toBeNull();
  });

  it("maps known backend error codes to natural Gujarati", () => {
    expect(getGujaratiErrorMessage("authentication_failed")).toBe("ઈમેઈલ અથવા પાસવર્ડ ખોટો છે.");
    expect(getGujaratiErrorMessage("login_rate_limited")).toBe(
      "ઘણા નિષ્ફળ પ્રયાસો થયા છે. કૃપા કરીને થોડી મિનિટો પછી પ્રયત્ન કરો.",
    );
    expect(getGujaratiErrorMessage("unknown_code")).toBe(
      "હમણાં વિનંતી પૂર્ણ થઈ શકી નથી. કૃપા કરીને ફરી પ્રયત્ન કરો.",
    );
  });

  it("uses the CSRF header name returned by the server", async () => {
    const mockFetch = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ token: "csrf-token", headerName: "X-CSRF-TOKEN" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ success: true }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );

    await apiRequest("/test-endpoint", {
      method: "POST",
      body: { sample: "data" },
    });

    expect(mockFetch).toHaveBeenCalledTimes(2);
    expect(mockFetch.mock.calls[0][0]).toBe("/api/v1/admin/auth/csrf");
    const calledInit = mockFetch.mock.calls[1][1];
    const headers = calledInit?.headers as Headers;
    expect(headers.get("X-CSRF-TOKEN")).toBe("csrf-token");
  });
});
