import { useCallback } from "react";
import { useQuery, useQueryClient, type QueryClient } from "@tanstack/react-query";
import { apiRequest } from "../../api/client";
import { queryKeys } from "../../api/queryKeys";
import {
    DownloadResponse,
    GalleryAlbumResponse,
    GalleryImageResponse,
    MaterialResponse,
    NoticeResponse,
    PageResponse,
} from "../../api/types";

function fetchPublicNotices(page: number, size: number) {
    return apiRequest<PageResponse<NoticeResponse>>(
        `/api/v1/public/notices?page=${page}&size=${size}`,
    );
}

function fetchPublicMaterials(page: number, size: number) {
    return apiRequest<PageResponse<MaterialResponse>>(
        `/api/v1/public/materials?page=${page}&size=${size}`,
    );
}

function fetchPublicDownloads(page: number, size: number) {
    return apiRequest<PageResponse<DownloadResponse>>(
        `/api/v1/public/downloads?page=${page}&size=${size}`,
    );
}

function fetchPublicGalleryAlbums(page: number, size: number) {
    return apiRequest<PageResponse<GalleryAlbumResponse>>(
        `/api/v1/public/gallery/albums?page=${page}&size=${size}`,
    );
}

export function usePublicNotices(page = 0, size = 20) {
    return useQuery<PageResponse<NoticeResponse>>({
        queryKey: queryKeys.notices({ page, size }),
        queryFn: () => fetchPublicNotices(page, size),
    });
}

export function usePublicNotice(noticeId: string | undefined) {
    const queryClient = useQueryClient();
    const listKey = queryKeys.notices({ page: 0, size: 50 });

    return useQuery<NoticeResponse>({
        queryKey: queryKeys.notice(noticeId ?? ""),
        queryFn: () => apiRequest<NoticeResponse>(`/api/v1/public/notices/${noticeId}`),
        enabled: Boolean(noticeId),
        initialData: () =>
            queryClient
                .getQueryData<PageResponse<NoticeResponse>>(listKey)
                ?.items.find((notice) => notice.id === noticeId),
        initialDataUpdatedAt: () => queryClient.getQueryState(listKey)?.dataUpdatedAt,
    });
}

export function usePublicGalleryAlbums(page = 0, size = 20) {
    return useQuery<PageResponse<GalleryAlbumResponse>>({
        queryKey: queryKeys.galleryAlbums({ page, size }),
        queryFn: () => fetchPublicGalleryAlbums(page, size),
    });
}

export function usePublicMaterials(page = 0, size = 50) {
    return useQuery<PageResponse<MaterialResponse>>({
        queryKey: queryKeys.materials({ page, size }),
        queryFn: () => fetchPublicMaterials(page, size),
    });
}

export function usePublicDownloads(page = 0, size = 50) {
    return useQuery<PageResponse<DownloadResponse>>({
        queryKey: queryKeys.downloads({ page, size }),
        queryFn: () => fetchPublicDownloads(page, size),
    });
}

export function prefetchPublicRoute(queryClient: QueryClient, pathname: string) {
    // Prefetch only public list data after an explicit mouse or keyboard navigation intent.
    // Student results, sessions, and admin data must never be prefetched.
    switch (pathname) {
        case "/notices":
            void queryClient.prefetchQuery({
                queryKey: queryKeys.notices({ page: 0, size: 20 }),
                queryFn: () => fetchPublicNotices(0, 20),
            });
            return;
        case "/student/materials":
            void queryClient.prefetchQuery({
                queryKey: queryKeys.materials({ page: 0, size: 50 }),
                queryFn: () => fetchPublicMaterials(0, 50),
            });
            return;
        case "/student/downloads":
            void queryClient.prefetchQuery({
                queryKey: queryKeys.downloads({ page: 0, size: 50 }),
                queryFn: () => fetchPublicDownloads(0, 50),
            });
            return;
        case "/gallery":
            void queryClient.prefetchQuery({
                queryKey: queryKeys.galleryAlbums({ page: 0, size: 20 }),
                queryFn: () => fetchPublicGalleryAlbums(0, 20),
            });
            return;
        default:
            return;
    }
}

export function usePublicRoutePrefetch() {
    const queryClient = useQueryClient();

    return useCallback(
        (pathname: string) => prefetchPublicRoute(queryClient, pathname),
        [queryClient],
    );
}

export function usePublicGalleryImages(albumId: string | undefined) {
    return useQuery<GalleryImageResponse[]>({
        queryKey: queryKeys.galleryAlbumImages(albumId || ""),
        queryFn: async () => {
            const res = await apiRequest<
                GalleryImageResponse[] | PageResponse<GalleryImageResponse>
            >(`/api/v1/public/gallery/albums/${albumId}/images`);
            return Array.isArray(res) ? res : res.items;
        },
        enabled: Boolean(albumId),
    });
}

export function usePublicGalleryImagesPaginated(albumId: string | undefined, page = 0, size = 20) {
    return useQuery<PageResponse<GalleryImageResponse>>({
        queryKey: queryKeys.galleryAlbumImages(albumId || "", { page, size }),
        queryFn: async () => {
            const res = await apiRequest<
                GalleryImageResponse[] | PageResponse<GalleryImageResponse>
            >(`/api/v1/public/gallery/albums/${albumId}/images?page=${page}&size=${size}`);
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
