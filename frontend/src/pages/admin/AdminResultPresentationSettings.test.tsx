import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import { AdminResultPresentationSettings } from "./AdminResultPresentationSettings";

vi.mock("@/api/client", () => ({
    apiRequest: vi.fn(),
}));

describe("AdminResultPresentationSettings - Flexible Grade Policy", () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        vi.clearAllMocks();
        queryClient = new QueryClient({
            defaultOptions: {
                queries: { retry: false },
                mutations: { retry: false },
            },
        });
    });

    const mockSettings = {
        resultDate: "2026-04-15",
        resultPlace: "ધધાણા",
        resultSheetTitle: "વાર્ષિક પરીક્ષા પરિણામ પત્રક : ૨૦૨૫-૨૬",
        footerLineOne: "નોંધ ૧",
        footerLineTwo: "નોંધ ૨",
        gradeAMin: 80,
        gradeBMin: 65,
        gradeCMin: 50,
        gradeDMin: 35,
        gradeBands: [
            { label: "A", minPercentage: 80 },
            { label: "B", minPercentage: 65 },
            { label: "C", minPercentage: 50 },
            { label: "D", minPercentage: 35 },
            { label: "F", minPercentage: 0 },
        ],
    };

    function renderComponent(tab: "ANNUAL" | "EKAM_KASOTI" = "ANNUAL") {
        vi.mocked(apiRequest).mockResolvedValue(mockSettings);
        queryClient.setQueryData(queryKeys.adminResultSettings("ANNUAL"), mockSettings);
        return render(
            <QueryClientProvider client={queryClient}>
                <AdminResultPresentationSettings settingsTab={tab} />
            </QueryClientProvider>,
        );
    }

    it("renders default flexible grade bands for Annual Exam settings", () => {
        renderComponent("ANNUAL");

        expect(screen.getByText("Flexible Grade Policy (ગ્રેડ નીતિ)")).toBeInTheDocument();
        expect(screen.getByDisplayValue("A")).toBeInTheDocument();
        expect(screen.getByDisplayValue("80")).toBeInTheDocument();
        expect(screen.getByDisplayValue("F")).toBeInTheDocument();
        expect(screen.getByDisplayValue("0")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: /Add Grade/i })).toBeInTheDocument();
    });

    it("allows adding a new grade band", () => {
        renderComponent("ANNUAL");

        fireEvent.click(screen.getByRole("button", { name: /Add Grade/i }));

        expect(screen.getByDisplayValue("Grade-6")).toBeInTheDocument();
    });

    it("allows removing a grade band", () => {
        renderComponent("ANNUAL");

        const removeButtons = screen.getAllByTitle("Remove grade band");
        expect(removeButtons.length).toBe(5);

        fireEvent.click(removeButtons[0]); // Remove 'A'

        expect(screen.queryByDisplayValue("A")).not.toBeInTheDocument();
    });

    it("validates missing 0% catch-all threshold on submit", () => {
        renderComponent("ANNUAL");
        vi.mocked(apiRequest).mockClear();

        // Change 'F' min percentage from 0 to 20
        const zeroInputs = screen.getAllByDisplayValue("0");
        fireEvent.change(zeroInputs[0], { target: { value: "20" } });

        fireEvent.click(screen.getByRole("button", { name: /Save Result Settings/i }));

        expect(
            screen.getByText("A catch-all grade band with 0% minimum is required."),
        ).toBeInTheDocument();
        // Ensure no PUT mutation request was sent
        expect(apiRequest).not.toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({ method: "PUT" }),
        );
    });

    it("hides grade policy editor on Trimasik Kasoti tab", () => {
        renderComponent("EKAM_KASOTI");

        expect(screen.queryByText("Flexible Grade Policy (ગ્રેડ નીતિ)")).not.toBeInTheDocument();
    });
});
