import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ResultsInfoPage } from "@/pages/ResultsInfoPage";

describe("ResultsInfoPage", () => {
  it("does not expose a fabricated result lookup before the secure workflow exists", () => {
    render(
      <MemoryRouter>
        <ResultsInfoPage />
      </MemoryRouter>,
    );

    expect(screen.getByText("પરિણામ સેવા ટૂંક સમયમાં ઉપલબ્ધ થશે")).toBeVisible();
    expect(screen.queryByLabelText(/રોલ નંબર/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/PIN/)).not.toBeInTheDocument();
  });
});
