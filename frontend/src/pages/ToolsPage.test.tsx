import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ToolsPage } from "./ToolsPage";

function renderToolsPage() {
  return render(
    <MemoryRouter>
      <ToolsPage />
    </MemoryRouter>,
  );
}

describe("ToolsPage", () => {
  const writeText = vi.fn();

  beforeEach(() => {
    writeText.mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText },
    });
  });

  it("renders every school tool", () => {
    renderToolsPage();
    [
      "ઉંમર ગણતરી",
      "તારીખ વચ્ચેનો તફાવત",
      "હાજરી ટકાવારી",
      "ગુણ ટકાવારી",
      "એકથી વધુ વિષયના ગુણ",
      "પાડા",
      "નંબરને શબ્દોમાં",
    ].forEach((title) => expect(screen.getByRole("heading", { name: title })).toBeInTheDocument());
  });

  it("calculates an age, copies its result, and resets the form", async () => {
    renderToolsPage();
    fireEvent.change(screen.getByLabelText("જન્મ તારીખ"), { target: { value: "2018-03-05" } });
    fireEvent.change(screen.getByLabelText("ઉંમર કઈ તારીખે"), { target: { value: "2024-06-17" } });
    fireEvent.click(screen.getAllByRole("button", { name: "ગણતરી કરો" })[0]!);

    expect(screen.getByText("કુલ મહિના:", { exact: false })).toHaveTextContent("75");
    fireEvent.click(screen.getByRole("button", { name: "પરિણામ કૉપી કરો" }));
    await waitFor(() =>
      expect(writeText).toHaveBeenCalledWith(expect.stringContaining("6 વર્ષ, 3 મહિના, 12 દિવસ")),
    );
    expect(screen.getByText("પરિણામ કૉપી થયું.")).toBeInTheDocument();

    fireEvent.click(screen.getAllByRole("button", { name: "ફરીથી શરૂ કરો" })[0]!);
    expect(screen.getByLabelText("જન્મ તારીખ")).toHaveValue("");
    expect(screen.queryByText("કુલ મહિના:", { exact: false })).not.toBeInTheDocument();
  });

  it("shows useful validation errors", () => {
    renderToolsPage();
    fireEvent.change(screen.getByLabelText("જન્મ તારીખ"), { target: { value: "2030-01-01" } });
    fireEvent.change(screen.getByLabelText("ઉંમર કઈ તારીખે"), { target: { value: "2024-01-01" } });
    fireEvent.click(screen.getAllByRole("button", { name: "ગણતરી કરો" })[0]!);
    expect(screen.getByRole("alert")).toHaveTextContent("જન્મ તારીખ પસંદ કરેલી તારીખ પછીની");
  });

  it("calculates attendance and does not persist calculation values", () => {
    const setItem = vi.spyOn(Storage.prototype, "setItem");
    renderToolsPage();
    fireEvent.change(screen.getByLabelText("કામકાજના દિવસ"), { target: { value: "220" } });
    fireEvent.change(screen.getByLabelText("હાજર દિવસ"), { target: { value: "205" } });
    fireEvent.click(screen.getAllByRole("button", { name: "ગણતરી કરો" })[2]!);

    expect(screen.getByText("હાજરી: 93.18%")).toBeInTheDocument();
    expect(setItem).not.toHaveBeenCalled();
    setItem.mockRestore();
  });

  it("adds and removes multi-subject rows", () => {
    renderToolsPage();
    expect(screen.getAllByLabelText("વિષયનું નામ (વૈકલ્પિક)")).toHaveLength(1);
    fireEvent.click(screen.getByRole("button", { name: "વિષય ઉમેરો" }));
    expect(screen.getAllByLabelText("વિષયનું નામ (વૈકલ્પિક)")).toHaveLength(2);
    fireEvent.click(screen.getByRole("button", { name: "વિષય 1 દૂર કરો" }));
    expect(screen.getAllByLabelText("વિષયનું નામ (વૈકલ્પિક)")).toHaveLength(1);
  });
});
