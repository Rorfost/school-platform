import { fireEvent, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it } from "vitest";
import { AdminAssessmentsPage } from "./AdminAssessmentsPage";

describe("AdminAssessmentsPage", () => {
  it("offers Exam and Trimasik Kasoti result tabs with the Exam uploader first", () => {
    const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <AdminAssessmentsPage />
      </QueryClientProvider>,
    );

    expect(screen.queryByText("Exam Setup")).not.toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Exam & Trimasik Kasoti Results" })).toBeVisible();
    expect(screen.getByRole("tab", { name: "Exam Result" })).toHaveAttribute(
      "aria-selected",
      "true",
    );
    expect(screen.getByRole("tab", { name: "Trimasik Kasoti Result" })).toHaveAttribute(
      "aria-selected",
      "false",
    );
    expect(screen.getByRole("heading", { name: "Upload Exam Result Workbook" })).toBeVisible();
    expect(screen.getByLabelText("Total working days")).toBeVisible();
  });

  it("hides working days on the Trimasik Kasoti tab", () => {
    const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <AdminAssessmentsPage />
      </QueryClientProvider>,
    );

    fireEvent.click(screen.getByRole("tab", { name: "Trimasik Kasoti Result" }));
    expect(
      screen.getByRole("heading", { name: "Upload Trimasik Kasoti Result Workbook" }),
    ).toBeVisible();
    expect(screen.queryByLabelText("Total working days")).not.toBeInTheDocument();
  });
});
