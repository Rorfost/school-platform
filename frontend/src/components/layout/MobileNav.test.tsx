import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { MobileNav } from "@/components/layout/MobileNav";

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

describe("MobileNav", () => {
    it("renders nothing when isOpen is false", () => {
        const onClose = vi.fn();
        renderWithProviders(<MobileNav isOpen={false} onClose={onClose} />);

        expect(screen.queryByRole("dialog", { name: "મુખ્ય મેનૂ" })).not.toBeInTheDocument();
    });

    it("renders dialog with close button and accessible navigation when open", () => {
        const onClose = vi.fn();
        renderWithProviders(<MobileNav isOpen={true} onClose={onClose} />);

        const dialog = screen.getByRole("dialog", { name: "મુખ્ય મેનૂ" });
        expect(dialog).toBeVisible();

        const closeBtn = screen.getByRole("button", { name: "મેનૂ બંધ કરો" });
        expect(closeBtn).toBeVisible();

        fireEvent.click(closeBtn);
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it("locks body overflow when open and restores it when closed", () => {
        const onClose = vi.fn();
        const { unmount } = renderWithProviders(<MobileNav isOpen={true} onClose={onClose} />);

        expect(document.body.style.overflow).toBe("hidden");

        unmount();
        expect(document.body.style.overflow).toBe("");
    });

    it("closes on Escape key press", () => {
        const onClose = vi.fn();
        renderWithProviders(<MobileNav isOpen={true} onClose={onClose} />);

        fireEvent.keyDown(window, { key: "Escape" });
        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
