import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AdminAssessmentsPage } from "./AdminAssessmentsPage";

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <AdminAssessmentsPage />
    </QueryClientProvider>,
  );
}

describe("AdminAssessmentsPage", () => {
  it("offers Exam and Trimasik Kasoti tabs with Annual Exam selected first", () => {
    renderPage();

    expect(screen.queryByText("Exam Setup")).not.toBeInTheDocument();

    expect(
      screen.getByRole("heading", {
        name: "Exam & Trimasik Kasoti Results",
      }),
    ).toBeVisible();

    expect(screen.getByRole("tab", { name: "Exam Result" })).toHaveAttribute(
      "aria-selected",
      "true",
    );

    expect(screen.getByRole("tab", { name: "Trimasik Kasoti Result" })).toHaveAttribute(
      "aria-selected",
      "false",
    );

    expect(
      screen.getByRole("heading", {
        name: "Upload Exam Result Workbook",
      }),
    ).toBeVisible();

    expect(screen.getByLabelText("Total working days")).toBeVisible();
  });

  it("hides working days on the Trimasik Kasoti tab", () => {
    renderPage();

    fireEvent.click(screen.getByRole("tab", { name: "Trimasik Kasoti Result" }));

    expect(
      screen.getByRole("heading", {
        name: "Upload Trimasik Kasoti Result Workbook",
      }),
    ).toBeVisible();

    expect(screen.queryByLabelText("Total working days")).not.toBeInTheDocument();
  });
});
