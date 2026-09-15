import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ContactPage } from "@/pages/ContactPage";

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("ContactPage", () => {
  it("renders school contact info and address in Gujarati", () => {
    renderWithProviders(<ContactPage />);

    expect(screen.getByRole("heading", { level: 1, name: "સંપર્ક" })).toBeVisible();
    expect(screen.getByText("પીએમ શ્રી ધધાણા પ્રાથમિક શાળા")).toBeVisible();
    expect(screen.getByText("principal24030401801@ssguj.in")).toBeVisible();
  });

  it("renders interactive contact enquiry form", () => {
    renderWithProviders(<ContactPage />);

    expect(screen.getByText("આપનું નામ")).toBeVisible();
    expect(screen.getByPlaceholderText("આપનો પ્રશ્ન અથવા સંદેશ અહીં લખો...")).toBeVisible();
    expect(screen.getByRole("button", { name: "સંદેશ મોકલો" })).toBeVisible();
  });
});
