import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { queryKeys } from "@/api/queryKeys";
import { AdminAcademicSetupPage } from "./AdminAcademicSetupPage";

describe("AdminAcademicSetupPage", () => {
  it("shows standards with their chosen subjects in one place", () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queryClient.setQueryData(queryKeys.adminAcademicSetup, {
      standards: [
        {
          standard: {
            id: "std-3",
            code: "STD_3",
            displayName: "Standard 3",
            sortOrder: 3,
            archived: false,
          },
          subjects: [
            { id: "maths", code: "MATHS", name: "Mathematics", sortOrder: 1, archived: false },
          ],
        },
      ],
      subjects: [
        { id: "maths", code: "MATHS", name: "Mathematics", sortOrder: 1, archived: false },
        { id: "evs", code: "EVS", name: "EVS", sortOrder: 2, archived: false },
      ],
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminAcademicSetupPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByRole("heading", { name: "Academic Setup" })).toBeVisible();
    expect(screen.getByRole("heading", { name: "Standard 3" })).toBeVisible();
    expect(screen.getByText("Mathematics")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Manage subjects" }));
    expect(screen.getByLabelText("EVS")).not.toBeChecked();
  });
});
