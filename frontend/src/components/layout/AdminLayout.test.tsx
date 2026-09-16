import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { AdminLayout } from "@/components/layout/AdminLayout";
import * as UseAuthModule from "@/features/auth/useAuth";

function renderAdminLayout() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/admin"]}>
        <AdminLayout />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AdminLayout", () => {
  it("renders header, navigation sidebar, and principal role badge", () => {
    vi.spyOn(UseAuthModule, "useAuth").mockReturnValue({
      principal: {
        id: "1",
        email: "principal@school.edu",
        role: "PRINCIPAL",
        mustChangePassword: false,
        schoolSlug: "dhadhana-primary",
      },
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetchSession: vi.fn(),
    });

    renderAdminLayout();

    expect(screen.getByText("Principal Admin Panel")).toBeInTheDocument();
    expect(screen.getByText("PRINCIPAL")).toBeInTheDocument();
    expect(screen.getByText("principal@school.edu")).toBeInTheDocument();
    expect(screen.getByRole("navigation", { name: /Admin Sidebar Navigation/i })).toBeInTheDocument();
  });

  it("renders warning banner when mustChangePassword is true", () => {
    vi.spyOn(UseAuthModule, "useAuth").mockReturnValue({
      principal: {
        id: "1",
        email: "principal@school.edu",
        role: "PRINCIPAL",
        mustChangePassword: true,
        schoolSlug: "dhadhana-primary",
      },
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetchSession: vi.fn(),
    });

    renderAdminLayout();

    expect(
      screen.getByText("Security Action Required: You must change your default password."),
    ).toBeInTheDocument();
    expect(screen.getByText("Change Password Now")).toBeInTheDocument();
  });

  it("toggles mobile menu drawer on menu button click", () => {
    vi.spyOn(UseAuthModule, "useAuth").mockReturnValue({
      principal: {
        id: "1",
        email: "principal@school.edu",
        role: "PRINCIPAL",
        mustChangePassword: false,
        schoolSlug: "dhadhana-primary",
      },
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetchSession: vi.fn(),
    });

    renderAdminLayout();

    const toggleButton = screen.getByLabelText("Toggle Navigation Menu");
    fireEvent.click(toggleButton);

    expect(screen.getByText("Navigation Menu")).toBeInTheDocument();
  });
});
