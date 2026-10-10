import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ResultsInfoPage } from "@/pages/ResultsInfoPage";

function renderResultsInfoPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
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
  it("renders the annual exam result search form", () => {
    renderResultsInfoPage();

    expect(
      screen.getByRole("heading", {
        name: "પરીક્ષા પરિણામ",
      }),
    ).toBeInTheDocument();

    expect(screen.getByLabelText("ધોરણ")).toBeInTheDocument();
    expect(screen.getByLabelText("વર્ગ")).toBeInTheDocument();
    expect(screen.getByLabelText("રોલ નંબર")).toBeInTheDocument();

    expect(screen.getByRole("button", { name: /પરિણામ જુઓ/ })).toBeInTheDocument();
  });

  it("disables the search button when inputs are empty", () => {
    renderResultsInfoPage();

    expect(screen.getByRole("button", { name: /પરિણામ જુઓ/ })).toBeDisabled();
  });

  it("offers annual and Trimasik result types", () => {
    renderResultsInfoPage();

    expect(
      screen.getByRole("option", {
        name: "પરીક્ષા પરિણામ",
      }),
    ).toBeInTheDocument();

    expect(
      screen.getByRole("option", {
        name: "ત્રિમાસિક કસોટી પરિણામ",
      }),
    ).toBeInTheDocument();
  });
});
