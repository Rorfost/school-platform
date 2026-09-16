import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { queryKeys } from "@/api/queryKeys";
import { AdminStandardsPage } from "./AdminStandardsPage";

function renderWithClient(ui: React.ReactElement, queryClient: QueryClient) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>
  );
}

describe("AdminStandardsPage", () => {
  it("renders standards list correctly", async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queryClient.setQueryData(queryKeys.adminStandards, [
      {
        id: "std-1",
        schoolId: "school-1",
        code: "STD_3",
        name: "ધોરણ ૩",
        displayOrder: 3,
        archived: false,
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
      },
    ]);

    renderWithClient(<AdminStandardsPage />, queryClient);

    expect(screen.getByRole("heading", { name: "Standards & Classes" })).toBeInTheDocument();
    expect(screen.getByText("STD_3")).toBeInTheDocument();
    expect(screen.getByText("ધોરણ ૩")).toBeInTheDocument();
  });
});
