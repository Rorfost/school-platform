import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { GalleryAlbumPage } from "@/pages/GalleryAlbumPage";
import { queryKeys } from "@/api/queryKeys";

const mockImages = [
    {
        id: "img-1",
        schoolId: "s1",
        albumId: "alb-1",
        url: "https://ik.imagekit.io/demo/photo1.jpg",
        thumbnailUrl: "https://ik.imagekit.io/demo/tr:w-480/photo1.jpg",
        caption: "વાર્ષિક ઉત્સવ તસવીર ૧",
        altText: "વાર્ષિક ઉત્સવ તસવીર ૧",
        sortOrder: 1,
        createdAt: "2026-01-01T00:00:00Z",
    },
    {
        id: "img-2",
        schoolId: "s1",
        albumId: "alb-1",
        url: "https://ik.imagekit.io/demo/photo2.jpg",
        thumbnailUrl: "https://ik.imagekit.io/demo/tr:w-480/photo2.jpg",
        caption: "વાર્ષિક ઉત્સવ તસવીર ૨",
        altText: "વાર્ષિક ઉત્સવ તસવીર ૨",
        sortOrder: 2,
        createdAt: "2026-01-01T00:00:00Z",
    },
];

function renderWithData() {
    const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false } },
    });

    queryClient.setQueryData(queryKeys.galleryAlbumImages("alb-1", { page: 0, size: 24 }), {
        items: mockImages,
        page: 0,
        size: 24,
        totalItems: 2,
        totalPages: 1,
    });

    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={["/gallery/alb-1"]}>
                <Routes>
                    <Route path="/gallery/:albumId" element={<GalleryAlbumPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe("GalleryAlbumPage Lightbox", () => {
    it("renders gallery album thumbnails", () => {
        renderWithData();

        expect(screen.getByText("વાર્ષિક ઉત્સવ તસવીર ૧")).toBeInTheDocument();
        expect(screen.getByText("વાર્ષિક ઉત્સવ તસવીર ૨")).toBeInTheDocument();
    });

    it("opens lightbox on thumbnail click, allows next/previous navigation, and closes on Escape", () => {
        renderWithData();

        // Open first image
        const firstCard = screen.getAllByRole("button", { name: /મોટી જુઓ/i })[0];
        fireEvent.click(firstCard);

        const dialog = screen.getByRole("dialog", { name: "તસવીર મોટી જુઓ" });
        expect(dialog).toBeVisible();
        expect(screen.getByText("1 / 2")).toBeVisible();

        // Navigate next
        const nextBtn = screen.getByRole("button", { name: "આગળની તસવીર" });
        fireEvent.click(nextBtn);
        expect(screen.getByText("2 / 2")).toBeVisible();

        // Navigate prev
        const prevBtn = screen.getByRole("button", { name: "પાછલી તસવીર" });
        fireEvent.click(prevBtn);
        expect(screen.getByText("1 / 2")).toBeVisible();

        // Close on Escape
        fireEvent.keyDown(window, { key: "Escape" });
        expect(screen.queryByRole("dialog", { name: "તસવીર મોટી જુઓ" })).not.toBeInTheDocument();
    });
});
