import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { HomePage } from "@/pages/HomePage";

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

describe("HomePage", () => {
  it("renders school identity and motto in Gujarati", () => {
    renderWithProviders(<HomePage />);

    expect(
      screen.getByRole("heading", { level: 1, name: "પીએમ શ્રી ધધાણા પ્રાથમિક શાળા" }),
    ).toBeVisible();
    expect(screen.getByText("॥ સા વિદ્યા યા વિમુક્તયે ॥")).toBeVisible();
  });

  it("renders student quick access section with friendly cards", () => {
    renderWithProviders(<HomePage />);

    expect(screen.getByRole("heading", { level: 2, name: "વિદ્યાર્થી વિભાગ" })).toBeVisible();
    expect(screen.getByText("અભ્યાસ સામગ્રી")).toBeVisible();
    expect(screen.getByText("પરીક્ષા પરિણામ")).toBeVisible();
    expect(screen.getAllByText("ડાઉનલોડ")[0]).toBeVisible();
  });

  it("renders school DISE code and establishment information", () => {
    renderWithProviders(<HomePage />);

    expect(screen.getAllByText(/ડાયસ કોડ:/)[0]).toBeVisible();
    expect(screen.getAllByText(/સ્થાપના:/)[0]).toBeVisible();
  });
});
