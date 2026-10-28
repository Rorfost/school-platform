import { render, screen, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { Button } from "./Button";
import { Card, CardHeader } from "./Card";
import { Input } from "./Input";

describe("Sprint 48 - Standardized UI Primitives", () => {
    describe("Button Component", () => {
        it("renders primary button with accessible brand tokens", () => {
            render(<Button variant="primary">મુખ્ય બટન</Button>);
            const button = screen.getByRole("button", { name: "મુખ્ય બટન" });
            expect(button).toBeInTheDocument();
            expect(button.className).toContain("bg-brand-900");
            expect(button.className).toContain("text-white");
            expect(button.className).toContain("min-h-11");
        });

        it("handles loading state with aria-busy and spinner", () => {
            render(
                <Button loading loadingText="સંગ્રહ થઈ રહ્યો છે...">
                    સાચવો
                </Button>,
            );
            const button = screen.getByRole("button");
            expect(button).toHaveAttribute("aria-busy", "true");
            expect(button).toBeDisabled();
            expect(screen.getByText("સંગ્રહ થઈ રહ્યો છે...")).toBeInTheDocument();
        });

        it("fires click events when active", () => {
            const handleClick = vi.fn();
            render(<Button onClick={handleClick}>ક્લિક કરો</Button>);
            fireEvent.click(screen.getByRole("button", { name: "ક્લિક કરો" }));
            expect(handleClick).toHaveBeenCalledTimes(1);
        });
    });

    describe("Card Component", () => {
        it("renders default and interactive variants with token styling", () => {
            const { rerender } = render(<Card variant="default">કાર્ડ વિગત</Card>);
            expect(screen.getByText("કાર્ડ વિગત").className).toContain("bg-white");

            rerender(<Card variant="interactive">ક્રિયાશીલ કાર્ડ</Card>);
            const interactive = screen.getByText("ક્રિયાશીલ કાર્ડ");
            expect(interactive.className).toContain("hover:border-brand-900");
            expect(interactive.className).toContain("cursor-pointer");
        });

        it("renders CardHeader with title, subtitle, and action", () => {
            render(
                <CardHeader
                    title="વિભાગ શીર્ષક"
                    subtitle="વિભાગ વર્ણન"
                    action={<button type="button">ક્રિયા</button>}
                />,
            );
            expect(screen.getByText("વિભાગ શીર્ષક")).toBeInTheDocument();
            expect(screen.getByText("વિભાગ વર્ણન")).toBeInTheDocument();
            expect(screen.getByRole("button", { name: "ક્રિયા" })).toBeInTheDocument();
        });
    });

    describe("Input Component", () => {
        it("renders input with accessible label and 44px touch height", () => {
            render(<Input label="વિદ્યાર્થી રોલ નંબર" placeholder="દા.ત. 12" />);
            const input = screen.getByLabelText("વિદ્યાર્થી રોલ નંબર");
            expect(input).toBeInTheDocument();
            expect(input.className).toContain("min-h-11");
            expect(input.className).toContain("focus-visible:ring-brand-900");
        });

        it("binds error message with aria-invalid and aria-describedby", () => {
            render(<Input label="પાસવર્ડ" error="પાસવર્ડ જરૂરી છે." id="test-pwd" />);
            const input = screen.getByLabelText("પાસવર્ડ");
            expect(input).toHaveAttribute("aria-invalid", "true");
            expect(input).toHaveAttribute("aria-describedby", "test-pwd-error");
            expect(screen.getByRole("alert")).toHaveTextContent("પાસવર્ડ જરૂરી છે.");
        });
    });
});
