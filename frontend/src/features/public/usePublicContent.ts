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
      apiRequest<PageResponse<NoticeResponse>>(`/api/v1/public/notices?page=${page}&size=${size}`),
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
    queryFn: () =>
      apiRequest<GalleryImageResponse[]>(`/api/v1/public/gallery/albums/${albumId}/images`),
    enabled: Boolean(albumId),
  });
}
