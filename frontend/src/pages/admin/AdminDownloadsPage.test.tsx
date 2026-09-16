import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { queryKeys } from "@/api/queryKeys";
import { AdminDownloadsPage } from "./AdminDownloadsPage";

function renderWithClient(ui: React.ReactElement, queryClient: QueryClient) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AdminDownloadsPage", () => {
  it("renders downloads document list correctly", async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queryClient.setQueryData(queryKeys.adminDownloads({ page: 0, size: 50 }), {
      items: [
        {
          id: "dl-1",
          schoolId: "school-1",
          title: "Admission Form 2026",
          description: "Student registration form",
          category: "FORM",
          filename: "admission_form.pdf",
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

    renderWithClient(<AdminDownloadsPage />, queryClient);

    expect(screen.getByRole("heading", { name: "Downloads Management" })).toBeInTheDocument();
    expect(screen.getByText("Admission Form 2026")).toBeInTheDocument();
    expect(screen.getByText("FORM")).toBeInTheDocument();
  });
});
