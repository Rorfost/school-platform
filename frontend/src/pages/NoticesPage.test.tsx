import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { NoticesPage } from "@/pages/NoticesPage";

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

describe("NoticesPage", () => {
  it("renders page header in Gujarati", () => {
    renderWithProviders(<NoticesPage />);

    expect(screen.getByRole("heading", { level: 1, name: "સૂચનાઓ" })).toBeVisible();
  });

  it("renders search input for filtering notices", () => {
    renderWithProviders(<NoticesPage />);

    expect(screen.getByPlaceholderText("સૂચનાઓમાં શોધો...")).toBeVisible();
  });
});
