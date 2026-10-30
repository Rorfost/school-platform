import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ConfirmModal } from "@/components/ui/ConfirmModal";

describe("ConfirmModal", () => {
    it("renders nothing when isOpen is false", () => {
        render(
            <ConfirmModal
                isOpen={false}
                onClose={vi.fn()}
                onConfirm={vi.fn()}
                title="Delete Record"
                description="Are you sure you want to delete this record?"
            />,
        );

        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });

    it("renders modal with title, description, and focusable controls when open", () => {
        const onClose = vi.fn();
        const onConfirm = vi.fn();

        render(
            <ConfirmModal
                isOpen={true}
                onClose={onClose}
                onConfirm={onConfirm}
                title="Delete Student"
                description="This action cannot be undone."
            />,
        );

        expect(screen.getByRole("dialog")).toBeVisible();
        expect(screen.getByText("Delete Student")).toBeVisible();
        expect(screen.getByText("This action cannot be undone.")).toBeVisible();

        const confirmBtn = screen.getByRole("button", { name: "Confirm" });
        fireEvent.click(confirmBtn);
        expect(onConfirm).toHaveBeenCalledTimes(1);

        const cancelBtn = screen.getByRole("button", { name: "Cancel" });
        fireEvent.click(cancelBtn);
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it("locks body overflow when open and restores it when unmounted", () => {
        const { unmount } = render(
            <ConfirmModal
                isOpen={true}
                onClose={vi.fn()}
                onConfirm={vi.fn()}
                title="Action"
                description="Desc"
            />,
        );

        expect(document.body.style.overflow).toBe("hidden");

        unmount();
        expect(document.body.style.overflow).toBe("");
    });

    it("closes on Escape key press when not loading", () => {
        const onClose = vi.fn();
        render(
            <ConfirmModal
                isOpen={true}
                onClose={onClose}
                onConfirm={vi.fn()}
                title="Action"
                description="Desc"
                isLoading={false}
            />,
        );

        fireEvent.keyDown(window, { key: "Escape" });
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it("does not close on Escape key press when loading", () => {
        const onClose = vi.fn();
        render(
            <ConfirmModal
                isOpen={true}
                onClose={onClose}
                onConfirm={vi.fn()}
                title="Action"
                description="Desc"
                isLoading={true}
            />,
        );

        fireEvent.keyDown(window, { key: "Escape" });
        expect(onClose).not.toHaveBeenCalled();
    });
});
