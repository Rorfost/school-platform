import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { AdminLoginPage } from "@/features/auth/AdminLoginPage";
import * as UseAuthModule from "@/features/auth/useAuth";

function renderLoginPage(initialState?: unknown, mockAuth = {}) {
  vi.spyOn(UseAuthModule, "useAuth").mockReturnValue({
    principal: null,
    isAuthenticated: false,
    isLoading: false,
    login: vi.fn(),
    logout: vi.fn(),
    refetchSession: vi.fn(),
    ...mockAuth,
  });

  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[{ pathname: "/admin/login", state: initialState }]}>
        <AdminLoginPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AdminLoginPage", () => {
  it("renders login form and title", () => {
    renderLoginPage();

    expect(screen.getByText("Principal Admin Portal")).toBeInTheDocument();
    expect(screen.getByLabelText(/Email Address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Password/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Sign in to Admin/i })).toBeInTheDocument();
  });

  it("displays expired session notice when redirected due to session expiration", () => {
    renderLoginPage({ expired: true });

    expect(screen.getByText("Your session has expired. Please sign in again.")).toBeInTheDocument();
  });

  it("handles valid form submission and calls login handler", async () => {
    const mockLogin = vi.fn().mockResolvedValue({
      id: "1",
      email: "principal@school.edu",
      role: "PRINCIPAL",
      mustChangePassword: false,
      schoolSlug: "dhadhana-primary",
    });

    renderLoginPage(undefined, { login: mockLogin });

    fireEvent.change(screen.getByLabelText(/Email Address/i), {
      target: { value: "principal@school.edu" },
    });
    fireEvent.change(screen.getByLabelText(/Password/i), {
      target: { value: "SecurePassword123" },
    });

    const submitButton = screen.getByRole("button", { name: /Sign in to Admin/i });
    fireEvent.submit(submitButton.closest("form")!);

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith("principal@school.edu", "SecurePassword123");
    });
  });
});
