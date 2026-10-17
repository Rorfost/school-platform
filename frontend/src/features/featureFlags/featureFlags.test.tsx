import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { queryKeys } from "@/api/queryKeys";
import { FeatureRouteGuard } from "@/features/featureFlags/FeatureRouteGuard";
import { useFeatureFlags } from "@/features/featureFlags/hooks";

function TestComponent() {
    const { isFeatureEnabled } = useFeatureFlags();
    return (
        <div>
            <div data-testid="gallery-status">
                {isFeatureEnabled("GALLERY") ? "enabled" : "disabled"}
            </div>
            <div data-testid="timetable-status">
                {isFeatureEnabled("TIMETABLE") ? "enabled" : "disabled"}
            </div>
        </div>
    );
}

describe("Feature Flags Frontend Integration", () => {
    it("reports enabled and disabled flags according to server data", () => {
        const queryClient = new QueryClient({
            defaultOptions: { queries: { retry: false } },
        });

        queryClient.setQueryData(queryKeys.features, {
            GALLERY: true,
            TIMETABLE: false,
        });

        render(
            <QueryClientProvider client={queryClient}>
                <TestComponent />
            </QueryClientProvider>,
        );

        expect(screen.getByTestId("gallery-status")).toHaveTextContent("enabled");
        expect(screen.getByTestId("timetable-status")).toHaveTextContent("disabled");
    });

    it("guards route and renders children when feature is enabled", () => {
        const queryClient = new QueryClient({
            defaultOptions: { queries: { retry: false } },
        });

        queryClient.setQueryData(queryKeys.features, {
            GALLERY: true,
        });

        render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={["/gallery"]}>
                    <Routes>
                        <Route
                            path="/gallery"
                            element={
                                <FeatureRouteGuard featureKey="GALLERY">
                                    <div>Protected Gallery Page Content</div>
                                </FeatureRouteGuard>
                            }
                        />
                        <Route path="/404" element={<div>Not Found Fallback</div>} />
                    </Routes>
                </MemoryRouter>
            </QueryClientProvider>,
        );

        expect(screen.getByText("Protected Gallery Page Content")).toBeInTheDocument();
        expect(screen.queryByText("Not Found Fallback")).not.toBeInTheDocument();
    });

    it("guards route and redirects to fallback 404 when feature is disabled", () => {
        const queryClient = new QueryClient({
            defaultOptions: { queries: { retry: false } },
        });

        queryClient.setQueryData(queryKeys.features, {
            GALLERY: false,
        });

        render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={["/gallery"]}>
                    <Routes>
                        <Route
                            path="/gallery"
                            element={
                                <FeatureRouteGuard featureKey="GALLERY">
                                    <div>Protected Gallery Page Content</div>
                                </FeatureRouteGuard>
                            }
                        />
                        <Route path="/404" element={<div>Not Found Fallback</div>} />
                    </Routes>
                </MemoryRouter>
            </QueryClientProvider>,
        );

        expect(screen.queryByText("Protected Gallery Page Content")).not.toBeInTheDocument();
        expect(screen.getByText("Not Found Fallback")).toBeInTheDocument();
    });

    it("guards admin route and redirects to /admin fallback when disabled", () => {
        const queryClient = new QueryClient({
            defaultOptions: { queries: { retry: false } },
        });

        queryClient.setQueryData(queryKeys.features, {
            MATERIALS: false,
        });

        render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={["/admin/materials"]}>
                    <Routes>
                        <Route
                            path="/admin/materials"
                            element={
                                <FeatureRouteGuard featureKey="MATERIALS" fallbackTo="/admin">
                                    <div>Protected Admin Materials</div>
                                </FeatureRouteGuard>
                            }
                        />
                        <Route path="/admin" element={<div>Admin Dashboard Fallback</div>} />
                    </Routes>
                </MemoryRouter>
            </QueryClientProvider>,
        );

        expect(screen.queryByText("Protected Admin Materials")).not.toBeInTheDocument();
        expect(screen.getByText("Admin Dashboard Fallback")).toBeInTheDocument();
    });
});
