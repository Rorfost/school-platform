import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { StudentCornerPage } from "@/pages/StudentCornerPage";

function renderWithProviders(ui: React.ReactElement) {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
        },
    });

    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>{ui}</MemoryRouter>
        </QueryClientProvider>,
    );
}

describe("StudentCornerPage", () => {
    it("renders page header and navigation cards for student facilities", () => {
        renderWithProviders(<StudentCornerPage />);

        expect(
            screen.getByRole("heading", {
                level: 1,
                name: "વિદ્યાર્થી વિભાગ",
            }),
        ).toBeVisible();

        expect(
            screen.getByRole("heading", {
                level: 2,
                name: "અભ્યાસ સામગ્રી",
            }),
        ).toBeVisible();

        expect(
            screen.getByRole("heading", {
                level: 2,
                name: "પરીક્ષા પરિણામ",
            }),
        ).toBeVisible();

        expect(
            screen.getByRole("heading", {
                level: 2,
                name: "ડાઉનલોડ",
            }),
        ).toBeVisible();

        expect(
            screen.getByRole("heading", {
                level: 2,
                name: "શાળા ઉપયોગી સાધનો",
            }),
        ).toBeVisible();
    });
});
