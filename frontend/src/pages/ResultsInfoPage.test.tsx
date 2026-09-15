import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ResultsInfoPage } from "@/pages/ResultsInfoPage";

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

describe("ResultsInfoPage", () => {
  it("renders result lookup form and Gujarati privacy notice", () => {
    renderWithProviders(<ResultsInfoPage />);

    expect(screen.getByText("પરિણામ ચકાસો")).toBeVisible();
    expect(screen.getByText("સુરક્ષિત અને ગોપનીય પરિણામ સિસ્ટમ")).toBeVisible();
    expect(screen.getByLabelText(/રોલ નંબર/)).toBeVisible();
    expect(screen.getByLabelText(/રિઝલ્ટ PIN/)).toBeVisible();
    expect(screen.getByRole("button", { name: "પરિણામ જુઓ" })).toBeVisible();
  });

  it("shows generic security failure message when invalid credentials are submitted", async () => {
    renderWithProviders(<ResultsInfoPage />);

    const stdSelect = screen.getByLabelText(/ધોરણ પસંદ કરો/);
    const rollInput = screen.getByLabelText(/રોલ નંબર/);
    const pinInput = screen.getByLabelText(/રિઝલ્ટ PIN/);
    const submitBtn = screen.getByRole("button", { name: "પરિણામ જુઓ" });

    fireEvent.change(stdSelect, { target: { value: "std3" } });
    fireEvent.change(rollInput, { target: { value: "999" } });
    fireEvent.change(pinInput, { target: { value: "0000" } });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText("આ માહિતી માટે પરિણામ મળ્યું નથી. કૃપા કરીને વિગતો ફરી તપાસો.")
      ).toBeVisible();
    });
  });

  it("renders student marksheet when valid test roll and PIN are submitted", async () => {
    renderWithProviders(<ResultsInfoPage />);

    const stdSelect = screen.getByLabelText(/ધોરણ પસંદ કરો/);
    const rollInput = screen.getByLabelText(/રોલ નંબર/);
    const pinInput = screen.getByLabelText(/રિઝલ્ટ PIN/);
    const submitBtn = screen.getByRole("button", { name: "પરિણામ જુઓ" });

    fireEvent.change(stdSelect, { target: { value: "std3" } });
    fireEvent.change(rollInput, { target: { value: "10" } });
    fireEvent.change(pinInput, { target: { value: "1234" } });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText("વિદ્યાર્થી ગુણપત્રક (પરિણામ)")).toBeVisible();
      expect(screen.getByText("પટેલ આયુષકુમાર રમેશભાઈ")).toBeVisible();
      expect(screen.getByText("ત્રિમાસિક એકમ કસોટી ૨૦૨૬-૨૭ (૨૦૨૬-૨૭)")).toBeVisible();
      expect(screen.getByText("ગુજરાતી")).toBeVisible();
      expect(screen.getByText("ગણિત")).toBeVisible();
      expect(screen.getByText("પર્યાવરણ")).toBeVisible();
      expect(screen.getByText("અંગ્રેજી")).toBeVisible();
      expect(screen.getByRole("button", { name: "પ્રિન્ટ કરો (છાપો)" })).toBeVisible();
    });
  });
});
