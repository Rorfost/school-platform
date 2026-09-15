import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { AdminDashboardPage } from "@/pages/admin/AdminDashboardPage";

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

describe("AdminDashboardPage", () => {
  it("renders welcome header and operational navigation shortcuts", () => {
    renderWithProviders(<AdminDashboardPage />);

    expect(screen.getByText("Administrative Control Panel")).toBeVisible();
    expect(screen.getByText("Operational Management")).toBeVisible();
    expect(screen.getByText("School Identity")).toBeVisible();
    expect(screen.getByText("Principal Profile")).toBeVisible();
    expect(screen.getByText("Academic Years")).toBeVisible();
    expect(screen.getByText("Standards & Classes")).toBeVisible();
    expect(screen.getByText("Subjects Catalog")).toBeVisible();
    expect(screen.getByText("Assessments")).toBeVisible();
  });
});
