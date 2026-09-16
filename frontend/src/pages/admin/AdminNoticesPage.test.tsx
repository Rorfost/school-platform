import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { queryKeys } from "@/api/queryKeys";
import { AdminNoticesPage } from "./AdminNoticesPage";

function renderWithClient(ui: React.ReactElement, queryClient: QueryClient) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>
  );
}

describe("AdminNoticesPage", () => {
  it("renders notice board list correctly", async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queryClient.setQueryData(queryKeys.notices({ page: 0, size: 50 }), {
      items: [
        {
          id: "notice-1",
          schoolId: "school-1",
          title: "Diwali Vacation Notice",
          body: "School will remain closed from Nov 1 to Nov 15.",
          pinned: true,
          status: "PUBLISHED",
          expiresAt: "2026-11-15T23:59:59Z",
          publishedAt: "2026-10-25T00:00:00Z",
          createdAt: "2026-10-25T00:00:00Z",
          updatedAt: "2026-10-25T00:00:00Z",
        },
      ],
      page: 0,
      size: 50,
      totalItems: 1,
      totalPages: 1,
    });

    renderWithClient(<AdminNoticesPage />, queryClient);

    expect(screen.getByRole("heading", { name: "Notice Board Management" })).toBeInTheDocument();
    expect(screen.getByText("Diwali Vacation Notice")).toBeInTheDocument();
    expect(screen.getByText("PINNED")).toBeInTheDocument();
  });
});
