import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { queryKeys } from "@/api/queryKeys";
import type { PrincipalAccountResponse } from "@/api/types";
import { AuthProvider } from "./AuthContext";
import { useAuth } from "./useAuth";

const { apiRequestMock, refreshCsrfTokenMock } = vi.hoisted(() => ({
    apiRequestMock: vi.fn(),
    refreshCsrfTokenMock: vi.fn(),
}));

vi.mock("@/api/client", async (importOriginal) => {
    const actual = await importOriginal<typeof import("@/api/client")>();
    return {
        ...actual,
        apiRequest: apiRequestMock,
        refreshCsrfToken: refreshCsrfTokenMock,
    };
});

const principalA: PrincipalAccountResponse = {
    id: "principal-a",
    email: "a@example.test",
    role: "PRINCIPAL",
    mustChangePassword: false,
    schoolSlug: "school-a",
};

const principalB: PrincipalAccountResponse = {
    ...principalA,
    id: "principal-b",
    email: "b@example.test",
};

function AuthControls() {
    const auth = useAuth();

    return (
        <div>
            <span data-testid="principal">{auth.principal?.id ?? "signed-out"}</span>
            <button onClick={() => void auth.logout()}>Log out</button>
            <button onClick={() => void auth.login("b@example.test", "password").catch(() => {})}>
                Switch account
            </button>
        </div>
    );
}

describe("AuthProvider query lifecycle", () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        vi.clearAllMocks();
        queryClient = new QueryClient({
            defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
        });
        apiRequestMock.mockImplementation(async (path: string) => {
            if (path.endsWith("/auth/me")) return principalA;
            if (path.endsWith("/auth/logout")) return undefined;
            if (path.endsWith("/auth/login")) return principalB;
            throw new Error(`Unexpected API request: ${path}`);
        });
        refreshCsrfTokenMock.mockResolvedValue(null);
    });

    it("removes private queries on logout while retaining public tenant data", async () => {
        queryClient.setQueryData(queryKeys.adminDashboardSummary, { secret: "private" });
        queryClient.setQueryData(queryKeys.school, { name: "Synthetic School A" });

        render(
            <QueryClientProvider client={queryClient}>
                <AuthProvider>
                    <AuthControls />
                </AuthProvider>
            </QueryClientProvider>,
        );

        await screen.findByText("principal-a");
        fireEvent.click(screen.getByRole("button", { name: "Log out" }));

        await waitFor(() => {
            expect(queryClient.getQueryData(queryKeys.adminDashboardSummary)).toBeUndefined();
        });
        expect(queryClient.getQueryData(queryKeys.school)).toEqual({ name: "Synthetic School A" });
        expect(screen.getByTestId("principal")).toHaveTextContent("signed-out");
    });

    it("clears the previous principal's private cache before switching accounts", async () => {
        queryClient.setQueryData(queryKeys.adminDashboardSummary, { secret: "principal-a" });

        render(
            <QueryClientProvider client={queryClient}>
                <AuthProvider>
                    <AuthControls />
                </AuthProvider>
            </QueryClientProvider>,
        );

        await screen.findByText("principal-a");
        fireEvent.click(screen.getByRole("button", { name: "Switch account" }));

        await screen.findByText("principal-b");
        expect(queryClient.getQueryData(queryKeys.adminDashboardSummary)).toBeUndefined();
    });
});
