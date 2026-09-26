import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it } from "vitest";
import { AdminAssessmentsPage } from "./AdminAssessmentsPage";

describe("AdminAssessmentsPage", () => {
  it("offers one English result uploader with both supported result types", () => {
    const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <AdminAssessmentsPage />
      </QueryClientProvider>,
    );

    expect(screen.queryByText("Exam Setup")).not.toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Exam & Ekam Kasoti Results" })).toBeVisible();
    expect(screen.getByLabelText("Result type")).toHaveValue("ANNUAL");
    expect(screen.getByRole("option", { name: "Exam Result" })).toBeVisible();
    expect(screen.getByRole("option", { name: "Ekam Kasoti Result" })).toBeVisible();
  });
});
