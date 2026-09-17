import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ContactPage } from "@/pages/ContactPage";

function renderContactPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter><ContactPage /></MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("ContactPage", () => {
  it("offers direct call and WhatsApp actions instead of a nonfunctional enquiry form", () => {
    renderContactPage();
    expect(screen.getByRole("link", { name: /કૉલ કરો/ })).toHaveAttribute("href", "tel:+919714862818");
    expect(screen.getByRole("link", { name: /WhatsApp/ })).toHaveAttribute("href", "https://wa.me/919714862818");
  });
});
