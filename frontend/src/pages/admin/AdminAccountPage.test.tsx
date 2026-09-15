import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import * as UseAuthModule from "@/features/auth/useAuth";
import { AdminAccountPage } from "@/pages/admin/AdminAccountPage";

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AdminAccountPage", () => {
  beforeEach(() => {
    vi.spyOn(UseAuthModule, "useAuth").mockReturnValue({
      principal: {
        id: "1",
        email: "principal24030401801@ssguj.in",
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
  });

  it("renders principal account details and change password form", () => {
    renderWithProviders(<AdminAccountPage />);

    expect(screen.getByText("Account Security & Credentials")).toBeVisible();
    expect(screen.getByText("Change Account Password")).toBeVisible();
    expect(screen.getByLabelText("Current Password")).toBeVisible();
    expect(screen.getByLabelText("New Password (min 12 characters)")).toBeVisible();
    expect(screen.getByLabelText("Confirm New Password")).toBeVisible();
  });

  it("validates minimum 12 characters for new password", async () => {
    renderWithProviders(<AdminAccountPage />);

    const currentInput = screen.getByLabelText("Current Password");
    const newInput = screen.getByLabelText("New Password (min 12 characters)");
    const confirmInput = screen.getByLabelText("Confirm New Password");
    const submitBtn = screen.getByRole("button", { name: "Update Password" });

    fireEvent.change(currentInput, { target: { value: "oldpassword123" } });
    fireEvent.change(newInput, { target: { value: "short" } });
    fireEvent.change(confirmInput, { target: { value: "short" } });
    fireEvent.click(submitBtn);

    expect(
      screen.getByText("New password must be at least 12 characters long.")
    ).toBeVisible();
  });
});
