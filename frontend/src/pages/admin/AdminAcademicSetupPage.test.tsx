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

    expect(screen.getByRole("heading", { name: "Standards & Subjects" })).toBeVisible();
    expect(screen.getByRole("heading", { name: "Standard 3" })).toBeVisible();
    expect(screen.getAllByText("Mathematics")[0]).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Manage Subjects" }));
    expect(screen.getByLabelText("EVS")).not.toBeChecked();
  });

  it("offers clear local dialogs for creating and deleting academic items", () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queryClient.setQueryData(queryKeys.adminAcademicSetup, {
      standards: [],
      subjects: [
        { id: "maths", code: "MATHS", name: "Mathematics", sortOrder: 1, archived: false },
      ],
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminAcademicSetupPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.click(screen.getAllByRole("button", { name: "Add Standard" })[0]);
    expect(screen.getByRole("dialog", { name: "Add Standard" })).toBeVisible();
    expect(screen.getByLabelText("Standard Name")).toHaveFocus();
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
    fireEvent.click(screen.getByRole("button", { name: "Delete Subject" }));
    expect(screen.getByRole("dialog", { name: "Delete Subject?" })).toBeVisible();
  });
});
