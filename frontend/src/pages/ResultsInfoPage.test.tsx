import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ResultsInfoPage } from "@/pages/ResultsInfoPage";

function renderResultsInfoPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ResultsInfoPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("ResultsInfoPage", () => {
  it("renders the exam result search form with standard and roll number inputs", () => {
    renderResultsInfoPage();

    expect(screen.getByRole("heading", { name: "પરીક્ષા પરિણામ" })).toBeInTheDocument();
    expect(screen.getByLabelText("ધોરણ")).toBeInTheDocument();
    expect(screen.getByLabelText("રોલ નંબર")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /પરિણામ જુઓ/ })).toBeInTheDocument();
  });

  it("disables the search button when inputs are empty", () => {
    renderResultsInfoPage();

    const submitButton = screen.getByRole("button", { name: /પરિણામ જુઓ/ });
    expect(submitButton).toBeDisabled();
  });
});
