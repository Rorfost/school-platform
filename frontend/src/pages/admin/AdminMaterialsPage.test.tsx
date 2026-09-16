import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { queryKeys } from "@/api/queryKeys";
import { AdminMaterialsPage } from "./AdminMaterialsPage";

function renderWithClient(ui: React.ReactElement, queryClient: QueryClient) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AdminMaterialsPage", () => {
  it("renders study materials list correctly", async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queryClient.setQueryData(queryKeys.adminMaterials({ page: 0, size: 50 }), {
      items: [
        {
          id: "mat-1",
          schoolId: "school-1",
          title: "Std 3 Maths Chapter 1",
          materialType: "TEXTBOOK",
          filename: "maths_ch1.pdf",
          status: "PUBLISHED",
          publishedAt: "2026-01-01T00:00:00Z",
          createdAt: "2026-01-01T00:00:00Z",
          updatedAt: "2026-01-01T00:00:00Z",
        },
      ],
      page: 0,
      size: 50,
      totalItems: 1,
      totalPages: 1,
    });

    renderWithClient(<AdminMaterialsPage />, queryClient);

    expect(screen.getByRole("heading", { name: "Study Materials Management" })).toBeInTheDocument();
    expect(screen.getByText("Std 3 Maths Chapter 1")).toBeInTheDocument();
    expect(screen.getByText("TEXTBOOK")).toBeInTheDocument();
  });
});
