import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { queryKeys } from "@/api/queryKeys";
import { AdminAcademicYearsPage } from "./AdminAcademicYearsPage";

function renderWithClient(ui: React.ReactElement, queryClient: QueryClient) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AdminAcademicYearsPage", () => {
  it("renders academic years list correctly", async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queryClient.setQueryData(queryKeys.adminAcademicYears, [
      {
        id: "year-1",
        schoolId: "school-1",
        name: "2026-27",
        startDate: "2026-06-01",
        endDate: "2027-05-31",
        status: "CURRENT",
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
      },
    ]);

    renderWithClient(<AdminAcademicYearsPage />, queryClient);

    expect(screen.getByRole("heading", { name: "Academic Years" })).toBeInTheDocument();
    expect(screen.getByText("2026-27")).toBeInTheDocument();
    expect(screen.getByText("CURRENT")).toBeInTheDocument();
  });
});
