import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { AdminAssessmentsPage } from "./AdminAssessmentsPage";

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  queryClient.setQueryData(
    ["admin", "assessments"],
    [
      {
        id: "assessment-1",
        academicYearId: "year-1",
        standardId: "standard-3",
        assessmentTypeId: "type-1",
        title: "First unit test",
        description: null,
        assessmentDate: "2026-09-01",
        status: "DRAFT",
        subjects: [],
      },
      {
        id: "assessment-2",
        academicYearId: "year-1",
        standardId: "standard-4",
        assessmentTypeId: "type-1",
        title: "Second unit test",
        description: null,
        assessmentDate: null,
        status: "PUBLISHED",
        subjects: [],
      },
    ],
  );
  queryClient.setQueryData(
    ["admin", "academic-years"],
    [
      {
        id: "year-1",
        name: "2026-27",
        startDate: "2026-06-01",
        endDate: "2027-05-31",
        status: "CURRENT",
      },
    ],
  );
  queryClient.setQueryData(
    ["admin", "standards"],
    [
      { id: "standard-3", code: "STD_3", name: "Standard 3", displayOrder: 3 },
      { id: "standard-4", code: "STD_4", name: "Standard 4", displayOrder: 4 },
    ],
  );
  queryClient.setQueryData(
    ["admin", "assessment-types"],
    [{ id: "type-1", code: "UNIT_TEST", displayName: "Unit Test", sortOrder: 1 }],
  );
  queryClient.setQueryData(["admin", "subjects"], []);

  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <AdminAssessmentsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AdminAssessmentsPage", () => {
  it("filters assessments by standard and accurately explains the blocked result workflow", () => {
    renderPage();

    expect(screen.getByText("Result import is not available yet")).toBeVisible();
    expect(screen.getByText("First unit test")).toBeVisible();
    expect(screen.getByText("Second unit test")).toBeVisible();

    fireEvent.change(screen.getByLabelText("Filter by standard"), {
      target: { value: "standard-3" },
    });

    expect(screen.getByText("First unit test")).toBeVisible();
    expect(screen.queryByText("Second unit test")).not.toBeInTheDocument();
  });
});
