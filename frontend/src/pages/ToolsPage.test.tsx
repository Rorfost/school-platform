import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ToolsPage } from "./ToolsPage";

describe("ToolsPage", () => {
  it("renders the five requested calculators only", () => {
    render(
      <MemoryRouter>
        <ToolsPage />
      </MemoryRouter>,
    );

    expect(screen.getAllByRole("heading")).toHaveLength(6);
    expect(screen.queryByText(/multiplication/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/number to words/i)).not.toBeInTheDocument();
  });
});
