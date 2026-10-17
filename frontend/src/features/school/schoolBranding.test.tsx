import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { queryKeys } from "@/api/queryKeys";
import { Footer } from "@/components/layout/Footer";
import { Header } from "@/components/layout/Header";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";

function DynamicBrandingConsumer() {
    const school = useEffectiveSchoolInfo();
    return (
        <div>
            <span data-testid="school-name">{school.name}</span>
            <span data-testid="school-code">{school.schoolCode}</span>
            <span data-testid="school-address">{school.address}</span>
            <span data-testid="school-phone">{school.phone}</span>
            <span data-testid="school-email">{school.email}</span>
        </div>
    );
}

describe("Sprint 19 - School Branding and Generic Defaults", () => {
    it("renders generic neutral defaults when no school is loaded", () => {
        const queryClient = new QueryClient({
            defaultOptions: { queries: { retry: false } },
        });

        render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter>
                    <DynamicBrandingConsumer />
                    <Footer />
                </MemoryRouter>
            </QueryClientProvider>,
        );

        // School fields default to empty string, not Dhadhana hardcodes
        expect(screen.getByTestId("school-name")).toHaveTextContent("");
        expect(screen.getByTestId("school-code")).toHaveTextContent("");
        expect(screen.getByTestId("school-address")).toHaveTextContent("");
        expect(screen.getByTestId("school-phone")).toHaveTextContent("");
        expect(screen.getByTestId("school-email")).toHaveTextContent("");

        // Footer displays generic fallback and software credit
        expect(screen.getByText(/શાળા પોર્ટલ · સર્વાધિકાર સુરક્ષિત/)).toBeInTheDocument();
        expect(
            screen.getByText("Portal software developed by Raj Patel | Rorfost"),
        ).toBeInTheDocument();
        expect(screen.queryByText(/PM SHRI Dhadhana/)).not.toBeInTheDocument();
        expect(screen.queryByText(/Rakesh Patel/)).not.toBeInTheDocument();
    });

    it("renders dynamic school branding for Synthetic School B without leaking School 1 data", () => {
        const queryClient = new QueryClient({
            defaultOptions: { queries: { retry: false } },
        });

        queryClient.setQueryData(queryKeys.school, {
            id: "school-b-uuid",
            name: "શ્રી વિદ્યાભારતી વિદ્યાલય",
            shortName: "વિદ્યાભારતી",
            schoolCode: "24040100101",
            address: "સ્ટેશન રોડ, રાજકોટ",
            city: "રાજકોટ",
            taluka: "રાજકોટ",
            district: "રાજકોટ",
            state: "ગુજરાત",
            postalCode: "360001",
            email: "contact@vidyabharati.edu",
            phone: "9876543210",
            establishedYear: "1985",
            logoUrl: null,
            sealUrl: null,
        });

        render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter>
                    <Header />
                    <DynamicBrandingConsumer />
                    <Footer />
                </MemoryRouter>
            </QueryClientProvider>,
        );

        // Header and Consumer display School B identity
        expect(screen.getAllByText("શ્રી વિદ્યાભારતી વિદ્યાલય")[0]).toBeInTheDocument();
        expect(screen.getByTestId("school-code")).toHaveTextContent("24040100101");
        expect(screen.getByTestId("school-phone")).toHaveTextContent("9876543210");
        expect(screen.getByTestId("school-email")).toHaveTextContent("contact@vidyabharati.edu");

        // Footer properly attributes School B
        expect(
            screen.getByText(/© \d{4} શ્રી વિદ્યાભારતી વિદ્યાલય · સર્વાધિકાર સુરક્ષિત/),
        ).toBeInTheDocument();
        expect(
            screen.getByText("Portal software developed by Raj Patel | Rorfost"),
        ).toBeInTheDocument();

        // No School 1 (Dhadhana / Rakesh Patel) leakage
        expect(screen.queryByText(/Dhadhana/i)).not.toBeInTheDocument();
        expect(screen.queryByText(/Rakesh Patel/i)).not.toBeInTheDocument();
    });
});
