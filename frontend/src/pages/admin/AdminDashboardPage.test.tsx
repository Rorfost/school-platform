import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { AdminDashboardPage } from "@/pages/admin/AdminDashboardPage";
import * as UseAuthModule from "@/features/auth/useAuth";

function renderWithProviders(ui: React.ReactElement) {
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

  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AdminDashboardPage", () => {
  it("renders welcome header and operational navigation shortcuts", () => {
    renderWithProviders(<AdminDashboardPage />);

    expect(screen.getByText("Administrative Control Panel")).toBeVisible();
    expect(screen.getByText("Management Modules")).toBeVisible();
    expect(screen.getByText("School Identity")).toBeVisible();
    expect(screen.getByText("Principal Profile")).toBeVisible();
    expect(screen.getByText("Academic Setup")).toBeVisible();
    expect(screen.getByText("Results & Marks")).toBeVisible();
  });
});
