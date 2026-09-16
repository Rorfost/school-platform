import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { queryKeys } from "@/api/queryKeys";
import { AdminGalleryPage } from "./AdminGalleryPage";

function renderWithClient(ui: React.ReactElement, queryClient: QueryClient) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>
  );
}

describe("AdminGalleryPage", () => {
  it("renders photo gallery albums list correctly", async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queryClient.setQueryData(queryKeys.adminGalleryAlbums({ page: 0, size: 50 }), {
      items: [
        {
          id: "album-1",
          schoolId: "school-1",
          title: "Sports Day 2026",
          description: "Annual sports day celebration",
          status: "PUBLISHED",
          publishedAt: "2026-02-15T00:00:00Z",
          createdAt: "2026-02-15T00:00:00Z",
          updatedAt: "2026-02-15T00:00:00Z",
        },
      ],
      page: 0,
      size: 50,
      totalItems: 1,
      totalPages: 1,
    });

    renderWithClient(<AdminGalleryPage />, queryClient);

    expect(screen.getByRole("heading", { name: "Photo Gallery Management" })).toBeInTheDocument();
    expect(screen.getByText("Sports Day 2026")).toBeInTheDocument();
    expect(screen.getByText("Annual sports day celebration")).toBeInTheDocument();
  });
});
