import { fireEvent, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it } from "vitest";
import { AdminAssessmentsPage } from "./AdminAssessmentsPage";

describe("AdminAssessmentsPage", () => {
  it("shows only the annual and Ekam Kasoti result upload tabs", () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <AdminAssessmentsPage />
      </QueryClientProvider>,
    );

    const tabs = screen.getAllByRole("tab");
    expect(tabs).toHaveLength(2);
    expect(screen.queryByText("Exam Setup")).not.toBeInTheDocument();
    expect(screen.getByText("વાર્ષિક પરીક્ષા પરિણામ અપલોડ કરો")).toBeVisible();

    fireEvent.click(tabs[1]!);

    expect(screen.getByText("એકમ કસોટી પરિણામ અપલોડ કરો")).toBeVisible();
  });
});
