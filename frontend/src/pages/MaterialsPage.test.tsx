import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { queryKeys } from "@/api/queryKeys";
import type { MaterialResponse, PageResponse } from "@/api/types";
import { MaterialsPage } from "./MaterialsPage";

describe("MaterialsPage", () => {
  it("shows the standard and subject supplied by the public material API", () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const materials: PageResponse<MaterialResponse> = {
      items: [
        {
          id: "material-1",
          title: "Fractions",
          description: null,
          materialType: "WORKSHEET",
          academicYearId: null,
          standardSubjectId: "mapping-1",
          standardName: "Standard 3",
          subjectName: "Mathematics",
          filename: "fractions.pdf",
          contentType: "application/pdf",
          byteSize: 100,
          url: "https://example.test/fractions.pdf",
          status: "PUBLISHED",
        },
      ],
      page: 0,
      size: 50,
      totalItems: 1,
      totalPages: 1,
    };
    queryClient.setQueryData(queryKeys.materials({ page: 0, size: 50 }), materials);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <MaterialsPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText("Standard 3")).toBeInTheDocument();
    expect(screen.getByText("Mathematics")).toBeInTheDocument();
  });
});
