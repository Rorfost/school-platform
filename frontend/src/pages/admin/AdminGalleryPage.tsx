import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Camera,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Image as ImageIcon,
  Plus,
  Upload,
} from "lucide-react";
import { ApiError, apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type {
  GalleryAlbumRequest,
  GalleryAlbumResponse,
  GalleryImageResponse,
  PageResponse,
} from "@/api/types";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { LoadingState } from "@/components/common/StatusPanel";

export function AdminGalleryPage() {
  const queryClient = useQueryClient();
  const [isAlbumModalOpen, setIsAlbumModalOpen] = useState(false);
  const [uploadImageAlbumId, setUploadImageAlbumId] = useState<string | null>(null);
  const [expandedAlbumId, setExpandedAlbumId] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const { data, isLoading } = useQuery<PageResponse<GalleryAlbumResponse>>({
    queryKey: queryKeys.adminGalleryAlbums({ page: 0, size: 50 }),
    queryFn: () =>
      apiRequest<PageResponse<GalleryAlbumResponse>>("/api/v1/admin/gallery/albums?page=0&size=50"),
  });

  const albums = data?.items ?? [];

  const createAlbumMutation = useMutation({
    mutationFn: (payload: GalleryAlbumRequest) =>
      apiRequest<GalleryAlbumResponse>("/api/v1/admin/gallery/albums", {
        method: "POST",
        body: payload,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.adminGalleryAlbums() });
      setIsAlbumModalOpen(false);
      setErrorMessage(null);
    },
    onError: (err) => {
      setErrorMessage(err instanceof ApiError ? err.message : "Failed to create album.");
    },
  });

  const uploadImageMutation = useMutation({
    mutationFn: ({ albumId, formData }: { albumId: string; formData: FormData }) =>
      apiRequest<GalleryImageResponse>(`/api/v1/admin/gallery/albums/${albumId}/images`, {
        method: "POST",
        body: formData,
      }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.adminGalleryAlbumImages(variables.albumId),
      });
      setUploadImageAlbumId(null);
      setErrorMessage(null);
    },
    onError: (err) => {
      setErrorMessage(err instanceof ApiError ? err.message : "Failed to upload photo.");
    },
  });

  const publishAlbumMutation = useMutation({
    mutationFn: (albumId: string) =>
      apiRequest<void>(`/api/v1/admin/gallery/albums/${albumId}/publish`, {
        method: "POST",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.adminGalleryAlbums() });
    },
  });

  const handleCreateAlbumSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const title = String(formData.get("title") || "");
    const description = String(formData.get("description") || "");
    createAlbumMutation.mutate({ title, description: description || null });
  };

  const handleUploadImageSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!uploadImageAlbumId) return;
    const formData = new FormData(e.currentTarget);
    uploadImageMutation.mutate({ albumId: uploadImageAlbumId, formData });
  };

  if (isLoading) {
    return <LoadingState message="Loading photo gallery..." />;
  }

  return (
    <div className="space-y-6 max-w-6xl">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Photo Gallery Management</h1>
          <p className="text-sm text-slate-600 mt-1">
            Create photo albums, upload event photographs, and publish galleries to the public
            portal.
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => setIsAlbumModalOpen(true)}
          className="gap-2 shrink-0"
        >
          <Plus size={16} aria-hidden="true" />
          <span>New Photo Album</span>
        </Button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {albums.map((album) => (
          <Card key={album.id} className="flex flex-col justify-between p-5 space-y-4">
            <div>
              <div className="aspect-video rounded-lg bg-slate-100 flex items-center justify-center text-slate-400 mb-4 border border-slate-200">
                <ImageIcon size={36} aria-hidden="true" />
              </div>
              <div className="flex items-start justify-between gap-2 mb-2">
                <h3 className="font-bold text-slate-900 text-base">{album.title}</h3>
                {album.status === "PUBLISHED" ? (
                  <Badge variant="primary" size="sm" className="gap-1 shrink-0">
                    <CheckCircle2 size={11} />
                    <span>PUBLISHED</span>
                  </Badge>
                ) : (
                  <Badge variant="neutral" size="sm" className="shrink-0">
                    {album.status}
                  </Badge>
                )}
              </div>
              {album.description && (
                <p className="text-xs text-slate-600 line-clamp-2 leading-relaxed">
                  {album.description}
                </p>
              )}
            </div>

            <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setExpandedAlbumId(expandedAlbumId === album.id ? null : album.id)}
                className="gap-1 text-xs text-slate-600"
              >
                {expandedAlbumId === album.id ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                <span>{expandedAlbumId === album.id ? "Hide Photos" : "View Photos"}</span>
              </Button>
              <div className="flex items-center gap-1">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setUploadImageAlbumId(album.id)}
                  className="gap-1.5"
                >
                  <Upload size={14} />
                  <span>Add</span>
                </Button>
                {album.status !== "PUBLISHED" && (
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => publishAlbumMutation.mutate(album.id)}
                    loading={publishAlbumMutation.isPending}
                  >
                    Publish
                  </Button>
                )}
              </div>
            </div>

            {/* Expanded Photos Grid */}
            {expandedAlbumId === album.id && <AlbumImagesGrid albumId={album.id} />}
          </Card>
        ))}
        {albums.length === 0 && (
          <div className="col-span-full p-12 text-center text-slate-500 bg-white rounded-xl border border-slate-200">
            <Camera size={40} className="mx-auto mb-2 text-slate-400" />
            <p className="font-semibold text-slate-700">No gallery albums created</p>
            <p className="text-xs text-slate-500 mt-1">
              Click 'New Photo Album' to create your first album.
            </p>
          </div>
        )}
      </div>

      {/* Create Album Modal */}
      {isAlbumModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <h2 className="text-lg font-bold text-slate-900">Create Photo Album</h2>
            <form onSubmit={handleCreateAlbumSubmit} className="space-y-4">
              {errorMessage && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700 font-medium">
                  {errorMessage}
                </div>
              )}
              <Input label="Album Title" name="title" required placeholder="e.g. Sports Day 2026" />
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Album Description
                </label>
                <textarea
                  name="description"
                  rows={3}
                  className="w-full rounded-lg border border-slate-300 p-2.5 text-sm text-slate-900"
                  placeholder="Event details..."
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button
                  variant="outline"
                  size="sm"
                  type="button"
                  onClick={() => {
                    setIsAlbumModalOpen(false);
                    setErrorMessage(null);
                  }}
                >
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  type="submit"
                  loading={createAlbumMutation.isPending}
                >
                  Create Album
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Upload Image Modal */}
      {uploadImageAlbumId && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <h2 className="text-lg font-bold text-slate-900">Upload Photo to Album</h2>
            <form onSubmit={handleUploadImageSubmit} className="space-y-4">
              {errorMessage && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700 font-medium">
                  {errorMessage}
                </div>
              )}
              <Input
                label="Alt Text / Title"
                name="altText"
                required
                placeholder="e.g. Winners ceremony"
              />
              <Input label="Caption (Optional)" name="caption" placeholder="Photo description..." />
              <Input label="Sort Order" name="sortOrder" type="number" defaultValue={1} />
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Select Image File <span className="text-red-500">*</span>
                </label>
                <input
                  type="file"
                  name="file"
                  accept="image/*"
                  required
                  className="w-full text-xs text-slate-600 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:bg-blue-50 file:text-blue-900 file:font-semibold hover:file:bg-blue-100"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button
                  variant="outline"
                  size="sm"
                  type="button"
                  onClick={() => {
                    setUploadImageAlbumId(null);
                    setErrorMessage(null);
                  }}
                >
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  type="submit"
                  loading={uploadImageMutation.isPending}
                >
                  Upload Photo
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

function AlbumImagesGrid({ albumId }: { albumId: string }) {
  const { data: images = [], isLoading } = useQuery<GalleryImageResponse[]>({
    queryKey: queryKeys.adminGalleryAlbumImages(albumId),
    queryFn: () =>
      apiRequest<GalleryImageResponse[]>(`/api/v1/admin/gallery/albums/${albumId}/images`),
  });

  if (isLoading) {
    return <div className="text-xs text-slate-500 py-2">Loading photos...</div>;
  }

  if (images.length === 0) {
    return (
      <div className="text-xs text-slate-400 py-2 italic text-center">
        No photos in this album yet.
      </div>
    );
  }

  return (
    <div className="grid grid-cols-3 gap-2 pt-2">
      {images.map((img) => (
        <div
          key={img.id}
          className="relative aspect-square rounded-lg overflow-hidden bg-slate-100 border border-slate-200"
        >
          {img.thumbnailUrl || img.url ? (
            <img
              src={img.thumbnailUrl ?? img.url ?? undefined}
              alt={img.altText || "Gallery photo"}
              className="w-full h-full object-cover"
              loading="lazy"
              decoding="async"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-slate-400">
              <ImageIcon size={28} aria-hidden="true" />
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
