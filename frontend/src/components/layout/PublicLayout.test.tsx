import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { PublicLayout } from "@/components/layout/PublicLayout";

function renderLayout() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/"]}>
        <Routes>
          <Route element={<PublicLayout />}>
            <Route index element={<div>મુખ્ય સામગ્રી</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("PublicLayout", () => {
  it("renders skip link, header, main content, and footer", () => {
    renderLayout();

    expect(screen.getByText("મુખ્ય વિષયવસ્તુ પર જાઓ")).toBeInTheDocument();
    expect(screen.getAllByText("પીએમ શ્રી ધધાણા પ્રાથમિક શાળા")[0]).toBeVisible();
    expect(screen.getByText("મુખ્ય સામગ્રી")).toBeVisible();
    expect(screen.getByText(/principal24030401801@ssguj.in/)).toBeVisible();
    expect(window.scrollTo).toHaveBeenCalledWith({ top: 0, behavior: "auto" });
  });

  it("opens and closes mobile navigation drawer on toggle", () => {
    renderLayout();

    const menuButton = screen.getByRole("button", { name: "મુખ્ય મેનૂ ખોલો" });
    expect(menuButton).toBeVisible();

    // Click menu button
    fireEvent.click(menuButton);

    const dialog = screen.getByRole("dialog", { name: "મુખ્ય મેનૂ" });
    expect(dialog).toBeVisible();

    // Close menu
    const closeButton = screen.getByRole("button", { name: "મેનૂ બંધ કરો" });
    fireEvent.click(closeButton);

    expect(screen.queryByRole("dialog", { name: "મુખ્ય મેનૂ" })).not.toBeInTheDocument();
  });
});
