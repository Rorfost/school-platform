import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { GalleryAlbumResponse, GalleryImageResponse, PageResponse } from "@/api/types";
import { AdminGalleryAlbumPage } from "./AdminGalleryAlbumPage";

vi.mock("@/api/client", () => ({
  ApiError: class ApiError extends Error {},
  apiRequest: vi.fn(),
}));

const album: GalleryAlbumResponse = {
  id: "album-1",
  title: "Sports Day",
  description: null,
  coverImageId: "image-1",
  coverImageThumbnailUrl: "https://images.example/cover.jpg",
  imageCount: 2,
  status: "DRAFT",
};

const images: GalleryImageResponse[] = [
  {
    id: "image-1",
    altText: "Opening ceremony",
    caption: null,
    sortOrder: 1,
    url: "https://images.example/one.jpg",
    thumbnailUrl: "https://images.example/one-thumb.jpg",
    status: "DRAFT",
  },
  {
    id: "image-2",
    altText: "Relay race",
    caption: "Relay race",
    sortOrder: 2,
    url: "https://images.example/two.jpg",
    thumbnailUrl: "https://images.example/two-thumb.jpg",
    status: "DRAFT",
  },
];

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  queryClient.setQueryData<PageResponse<GalleryAlbumResponse>>(
    queryKeys.adminGalleryAlbums({ page: 0, size: 50 }),
    { items: [album], page: 0, size: 50, totalItems: 1, totalPages: 1 },
  );
  queryClient.setQueryData(queryKeys.adminGalleryAlbumImages("album-1"), images);
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/admin/gallery/album-1"]}>
        <Routes>
          <Route path="/admin/gallery/:albumId" element={<AdminGalleryAlbumPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AdminGalleryAlbumPage", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockImplementation((path) => {
      if (String(path).endsWith("/images")) return Promise.resolve(images);
      if (String(path).includes("albums?page"))
        return Promise.resolve({ items: [album], page: 0, size: 50, totalItems: 1, totalPages: 1 });
      return Promise.resolve(undefined);
    });
    Object.defineProperty(URL, "createObjectURL", {
      configurable: true,
      value: vi.fn(() => "blob:preview"),
    });
    Object.defineProperty(URL, "revokeObjectURL", { configurable: true, value: vi.fn() });
  });

  it("shows album photos, marks the first cover, and offers cover controls", () => {
    renderPage();
    expect(screen.getByRole("heading", { name: "Sports Day" })).toBeInTheDocument();
    expect(screen.getByText("Cover")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Set as Cover" })).toBeInTheDocument();
  });

  it("asks for confirmation before deleting a photo", () => {
    renderPage();
    fireEvent.click(screen.getAllByRole("button", { name: "Delete" })[0]);
    expect(screen.getByRole("dialog", { name: /delete photo/i })).toBeInTheDocument();
    expect(
      screen.getByText("This photo will be permanently removed from the album."),
    ).toBeInTheDocument();
  });

  it("selects multiple supported photos before upload", () => {
    renderPage();
    const input = screen
      .getAllByLabelText(/upload photos/i)
      .find((element) => element.tagName === "INPUT");
    const first = new File(["one"], "sports-day.jpg", { type: "image/jpeg" });
    const second = new File(["two"], "relay.webp", { type: "image/webp" });
    fireEvent.change(input!, { target: { files: [first, second] } });
    expect(screen.getByText("sports-day.jpg")).toBeInTheDocument();
    expect(screen.getByText("relay.webp")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Upload 2 Photos" })).toBeInTheDocument();
  });

  it("sends the reordered ids when moving a photo", async () => {
    renderPage();
    fireEvent.click(screen.getByRole("button", { name: "Move Relay race earlier" }));
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "/api/v1/admin/gallery/albums/album-1/images/reorder",
        expect.objectContaining({ method: "PATCH", body: { imageIds: ["image-2", "image-1"] } }),
      ),
    );
  });
});
