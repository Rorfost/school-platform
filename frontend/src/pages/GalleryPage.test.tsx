import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { queryKeys } from "@/api/queryKeys";
import type { GalleryAlbumResponse, PageResponse } from "@/api/types";
import { GalleryPage } from "./GalleryPage";

describe("GalleryPage", () => {
  it("renders the published album cover and photo count returned by the public gallery", () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const albums: PageResponse<GalleryAlbumResponse> = {
      items: [
        {
          id: "published-album",
          title: "Annual Day",
          description: "School celebration",
          coverImageId: "cover-1",
          coverImageThumbnailUrl: "https://images.example/annual-day-thumb.jpg",
          imageCount: 8,
          status: "PUBLISHED",
        },
      ],
      page: 0,
      size: 50,
      totalItems: 1,
      totalPages: 1,
    };
    queryClient.setQueryData(queryKeys.galleryAlbums({ page: 0, size: 50 }), albums);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <GalleryPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByRole("link", { name: /Annual Day/ })).toHaveAttribute(
      "href",
      "/gallery/published-album",
    );
    expect(screen.getByAltText("Annual Day")).toHaveAttribute(
      "src",
      "https://images.example/annual-day-thumb.jpg",
    );
    expect(screen.getByText("8 ફોટા")).toBeInTheDocument();
  });
});
