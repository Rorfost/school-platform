import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { HomePage } from "@/pages/HomePage";

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("HomePage", () => {
  it("keeps home focused on student actions rather than repeating school identity", () => {
    renderWithProviders(<HomePage />);

    expect(screen.queryByRole("heading", { level: 1 })).not.toBeInTheDocument();
  });

  it("renders student quick access section with friendly cards", () => {
    renderWithProviders(<HomePage />);

    expect(screen.getByRole("heading", { level: 2, name: "વિદ્યાર્થી વિભાગ" })).toBeVisible();
    expect(screen.getByText("અભ્યાસ સામગ્રી")).toBeVisible();
    expect(screen.getByText("પરીક્ષા પરિણામ")).toBeVisible();
    expect(screen.getAllByText("ડાઉનલોડ")[0]).toBeVisible();
  });
});
