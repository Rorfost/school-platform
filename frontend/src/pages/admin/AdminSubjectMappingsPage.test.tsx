import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { queryKeys } from "@/api/queryKeys";
import { AdminSubjectMappingsPage } from "./AdminSubjectMappingsPage";

function renderWithClient(ui: React.ReactElement, queryClient: QueryClient) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>
  );
}

describe("AdminSubjectMappingsPage", () => {
  it("renders standard subject mappings correctly", async () => {
    const queryClient = new QueryClient({ defaultOptions: { retry: false } });
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
    queryClient.setQueryData(queryKeys.adminStandardSubjects("std-1"), [
      {
        id: "map-1",
        schoolId: "school-1",
        standardId: "std-1",
        subjectId: "sub-1",
        sortOrder: 1,
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
      },
    ]);

    renderWithClient(<AdminSubjectMappingsPage />, queryClient);

    expect(screen.getByRole("heading", { name: "Standard-Subject Mapping" })).toBeInTheDocument();
    expect(screen.getByText("Mapped Subjects for ધોરણ ૩")).toBeInTheDocument();
    expect(screen.getByText("ગણિત")).toBeInTheDocument();
  });
});
