import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "../../api/client";
import { queryKeys } from "../../api/queryKeys";
import {
    GalleryAlbumResponse,
    GalleryImageResponse,
    NoticeResponse,
    PageResponse,
} from "../../api/types";

export function usePublicNotices(page = 0, size = 20) {
    return useQuery<PageResponse<NoticeResponse>>({
        queryKey: queryKeys.notices({ page, size }),
        queryFn: () =>
            apiRequest<PageResponse<NoticeResponse>>(
                `/api/v1/public/notices?page=${page}&size=${size}`,
            ),
    });
}

export function usePublicNotice(noticeId: string | undefined) {
    const { data: noticesPage, isLoading, error } = usePublicNotices(0, 50);
    const notice = noticesPage?.items.find((n) => n.id === noticeId);
    return {
        data: notice,
        isLoading,
        error,
    };
}

export function usePublicGalleryAlbums(page = 0, size = 20) {
    return useQuery<PageResponse<GalleryAlbumResponse>>({
        queryKey: queryKeys.galleryAlbums({ page, size }),
        queryFn: () =>
            apiRequest<PageResponse<GalleryAlbumResponse>>(
                `/api/v1/public/gallery/albums?page=${page}&size=${size}`,
            ),
    });
}

export function usePublicGalleryImages(albumId: string | undefined) {
    return useQuery<GalleryImageResponse[]>({
        queryKey: queryKeys.galleryAlbumImages(albumId || ""),
        queryFn: async () => {
            const res = await apiRequest<GalleryImageResponse[] | PageResponse<GalleryImageResponse>>(
                `/api/v1/public/gallery/albums/${albumId}/images`,
            );
            return Array.isArray(res) ? res : res.items;
        },
        enabled: Boolean(albumId),
    });
}

export function usePublicGalleryImagesPaginated(
    albumId: string | undefined,
    page = 0,
    size = 20,
) {
    return useQuery<PageResponse<GalleryImageResponse>>({
        queryKey: [...queryKeys.galleryAlbumImages(albumId || ""), { page, size }],
        queryFn: async () => {
            const res = await apiRequest<GalleryImageResponse[] | PageResponse<GalleryImageResponse>>(
                `/api/v1/public/gallery/albums/${albumId}/images?page=${page}&size=${size}`,
            );
            if (Array.isArray(res)) {
                return {
                    items: res,
                    page: 0,
                    size: res.length,
                    totalItems: res.length,
                    totalPages: 1,
                };
            }
            return res;
        },
        enabled: Boolean(albumId),
    });
}
