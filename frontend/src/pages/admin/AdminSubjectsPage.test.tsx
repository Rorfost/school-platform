import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { queryKeys } from "@/api/queryKeys";
import { AdminSubjectsPage } from "./AdminSubjectsPage";

function renderWithClient(ui: React.ReactElement, queryClient: QueryClient) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>
  );
}

describe("AdminSubjectsPage", () => {
  it("renders subjects list correctly", async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queryClient.setQueryData(queryKeys.adminSubjects, [
      {
        id: "sub-1",
        schoolId: "school-1",
        code: "MATHS",
        name: "ગણિત",
        displayOrder: 1,
        archived: false,
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
      },
    ]);

    renderWithClient(<AdminSubjectsPage />, queryClient);

    expect(screen.getByRole("heading", { name: "Subjects Catalog" })).toBeInTheDocument();
    expect(screen.getByText("MATHS")).toBeInTheDocument();
    expect(screen.getByText("ગણિત")).toBeInTheDocument();
  });
});
