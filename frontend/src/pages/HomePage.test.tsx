import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { HomePage } from "@/pages/HomePage";

describe("HomePage", () => {
  it("shows the Gujarati configuration placeholder", () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    );

    expect(screen.getByRole("heading", { name: "માહિતી ટૂંક સમયમાં ઉપલબ્ધ થશે." })).toBeVisible();
  });
});
