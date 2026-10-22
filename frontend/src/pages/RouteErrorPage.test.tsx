import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { RouteErrorPage } from "@/pages/RouteErrorPage";
import { LABELS } from "@/utils/gujarati";

vi.mock("react-router-dom", async (importOriginal) => {
    const actual = await importOriginal<typeof import("react-router-dom")>();
    return {
        ...actual,
        isRouteErrorResponse: vi.fn(() => false),
        useRouteError: vi.fn(() => new Error("ChunkLoadError")),
    };
});

describe("RouteErrorPage", () => {
    it("offers an accessible retry action when a route chunk fails to load", () => {
        render(
            <MemoryRouter>
                <RouteErrorPage />
            </MemoryRouter>,
        );

        expect(screen.getByRole("alert")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: LABELS.tryAgain })).toBeInTheDocument();
    });
});
