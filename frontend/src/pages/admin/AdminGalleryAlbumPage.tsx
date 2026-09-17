import { useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ArrowLeft,
  GripVertical,
  Image as ImageIcon,
  Pencil,
  Star,
  Trash2,
  Upload,
  X,
} from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { GalleryAlbumResponse, GalleryImageResponse, PageResponse } from "@/api/types";
import { LoadingState } from "@/components/common/StatusPanel";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ConfirmModal } from "@/components/ui/ConfirmModal";
import { Input } from "@/components/ui/Input";

const MAX_IMAGE_SIZE = 10 * 1024 * 1024;
const SUPPORTED_IMAGE_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);

type PendingImage = { id: string; file: File; previewUrl: string; altText: string };

export function AdminGalleryAlbumPage() {
  const { albumId } = useParams<{ albumId: string }>();
  const queryClient = useQueryClient();
  const [pendingImages, setPendingImages] = useState<PendingImage[]>([]);
  const [orderedImageIds, setOrderedImageIds] = useState<string[] | null>(null);
  const [draggedImageId, setDraggedImageId] = useState<string | null>(null);
  const [makeFirstUploadCover, setMakeFirstUploadCover] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<GalleryImageResponse | null>(null);
  const [editingImage, setEditingImage] = useState<GalleryImageResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const previewUrls = useRef(new Set<string>());

  const {
    data: albumData,
    isLoading: isAlbumLoading,
    error: albumError,
  } = useQuery<PageResponse<GalleryAlbumResponse>>({
    queryKey: queryKeys.adminGalleryAlbums({ page: 0, size: 50 }),
    queryFn: () =>
      apiRequest<PageResponse<GalleryAlbumResponse>>("/api/v1/admin/gallery/albums?page=0&size=50"),
  });
  const album = albumData?.items.find((item) => item.id === albumId);

  const {
    data: imagesData,
    isLoading: isImagesLoading,
    error: imagesError,
  } = useQuery<GalleryImageResponse[]>({
    queryKey: queryKeys.adminGalleryAlbumImages(albumId ?? ""),
    queryFn: () =>
      apiRequest<GalleryImageResponse[]>(`/api/v1/admin/gallery/albums/${albumId}/images`),
    enabled: Boolean(albumId),
  });

  const orderedImages = useMemo(() => {
    const currentImages = imagesData ?? [];
    if (!orderedImageIds) return currentImages;
    const imageById = new Map(currentImages.map((image) => [image.id, image]));
    const reordered = orderedImageIds
      .map((id) => imageById.get(id))
      .filter((image): image is GalleryImageResponse => Boolean(image));
    const reorderedIds = new Set(orderedImageIds);
    return [...reordered, ...currentImages.filter((image) => !reorderedIds.has(image.id))];
  }, [imagesData, orderedImageIds]);
  useEffect(
    () => () => {
      previewUrls.current.forEach((url) => URL.revokeObjectURL(url));
    },
    [],
  );

  const refreshGallery = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.adminGalleryAlbums() });
    queryClient.invalidateQueries({ queryKey: queryKeys.galleryAlbums() });
    if (albumId)
      queryClient.invalidateQueries({ queryKey: queryKeys.adminGalleryAlbumImages(albumId) });
  };

  const uploadMutation = useMutation({
    mutationFn: async (items: PendingImage[]) => {
      const formData = new FormData();
      items.forEach((item) => {
        formData.append("files", item.file);
        formData.append("altTexts", item.altText);
      });
      const uploaded = await apiRequest<GalleryImageResponse[]>(
        `/api/v1/admin/gallery/albums/${albumId}/images/batch`,
        {
          method: "POST",
          body: formData,
        },
      );
      if (makeFirstUploadCover && uploaded[0]) {
        await apiRequest(`/api/v1/admin/gallery/albums/${albumId}/cover`, {
          method: "PUT",
          body: { imageId: uploaded[0].id },
        });
      }
    },
    onSuccess: () => {
      clearPendingImages();
      setErrorMessage(null);
      refreshGallery();
    },
    onError: (error) => setErrorMessage(messageFor(error, "No photos were added. Try again.")),
  });

  const setCoverMutation = useMutation({
    mutationFn: (imageId: string) =>
      apiRequest(`/api/v1/admin/gallery/albums/${albumId}/cover`, {
        method: "PUT",
        body: { imageId },
      }),
    onSuccess: () => {
      setErrorMessage(null);
      refreshGallery();
    },
    onError: (error) => setErrorMessage(messageFor(error, "Could not change the cover photo.")),
  });

  const deleteMutation = useMutation({
    mutationFn: (imageId: string) =>
      apiRequest(`/api/v1/admin/gallery/albums/${albumId}/images/${imageId}`, { method: "DELETE" }),
    onSuccess: () => {
      setDeleteTarget(null);
      setErrorMessage(null);
      refreshGallery();
    },
    onError: (error) => setErrorMessage(messageFor(error, "Could not delete photo.")),
  });

  const updateImageMutation = useMutation({
    mutationFn: ({
      id,
      altText,
      caption,
    }: {
      id: string;
      altText: string;
      caption: string | null;
    }) =>
      apiRequest(`/api/v1/admin/gallery/albums/${albumId}/images/${id}`, {
        method: "PUT",
        body: { altText, caption },
      }),
    onSuccess: () => {
      setEditingImage(null);
      setErrorMessage(null);
      refreshGallery();
    },
    onError: (error) => setErrorMessage(messageFor(error, "Could not update photo details.")),
  });

  const reorderMutation = useMutation({
    mutationFn: (imageIds: string[]) =>
      apiRequest(`/api/v1/admin/gallery/albums/${albumId}/images/reorder`, {
        method: "PATCH",
        body: { imageIds },
      }),
    onSuccess: () => refreshGallery(),
  });

  const clearPendingImages = () => {
    pendingImages.forEach((item) => {
      URL.revokeObjectURL(item.previewUrl);
      previewUrls.current.delete(item.previewUrl);
    });
    setPendingImages([]);
    setMakeFirstUploadCover(false);
  };

  const addFiles = (files: FileList | File[]) => {
    const accepted: PendingImage[] = [];
    const rejected: string[] = [];
    Array.from(files).forEach((file) => {
      if (!SUPPORTED_IMAGE_TYPES.has(file.type) || file.size > MAX_IMAGE_SIZE) {
        rejected.push(file.name);
        return;
      }
      const previewUrl = URL.createObjectURL(file);
      previewUrls.current.add(previewUrl);
      accepted.push({
        id: `${file.name}-${file.lastModified}-${Math.random()}`,
        file,
        previewUrl,
        altText: humanizeFilename(file.name),
      });
    });
    if (accepted.length) setPendingImages((current) => [...current, ...accepted]);
    setErrorMessage(
      rejected.length
        ? `Only JPEG, PNG, and WebP images up to 10 MB can be uploaded. Skipped: ${rejected.join(", ")}`
        : null,
    );
  };

  const removePendingImage = (id: string) => {
    setPendingImages((current) => {
      const image = current.find((item) => item.id === id);
      if (image) {
        URL.revokeObjectURL(image.previewUrl);
        previewUrls.current.delete(image.previewUrl);
      }
      return current.filter((item) => item.id !== id);
    });
  };

  const submitReorder = (nextOrder: GalleryImageResponse[]) => {
    const previousOrder = orderedImages.map((image) => image.id);
    setOrderedImageIds(nextOrder.map((image) => image.id));
    reorderMutation.mutate(
      nextOrder.map((image) => image.id),
      {
        onError: (error) => {
          setOrderedImageIds(previousOrder);
          setErrorMessage(
            messageFor(error, "Could not save photo order. The previous order was restored."),
          );
        },
      },
    );
  };

  const moveImage = (imageId: string, targetIndex: number) => {
    const fromIndex = orderedImages.findIndex((image) => image.id === imageId);
    if (
      fromIndex < 0 ||
      targetIndex < 0 ||
      targetIndex >= orderedImages.length ||
      fromIndex === targetIndex
    )
      return;
    const nextOrder = [...orderedImages];
    const [moved] = nextOrder.splice(fromIndex, 1);
    nextOrder.splice(targetIndex, 0, moved);
    submitReorder(nextOrder);
  };

  if (isAlbumLoading || isImagesLoading) return <LoadingState message="Loading album photos..." />;
  if (albumError || imagesError || !album) {
    return (
      <div className="space-y-4 rounded-xl border border-red-200 bg-red-50 p-5 text-sm text-red-800">
        <p>Could not load this album. It may have been deleted.</p>
        <Link to="/admin/gallery" className="font-semibold underline">
          Return to gallery
        </Link>
      </div>
    );
  }
  const isReadOnly = album.status === "ARCHIVED";

  return (
    <div className="max-w-6xl space-y-6">
      <Link
        to="/admin/gallery"
        className="inline-flex items-center gap-2 text-sm font-semibold text-blue-900 hover:underline"
      >
        <ArrowLeft size={16} aria-hidden="true" /> Gallery
      </Link>
      <header className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-sm text-slate-500">Gallery &gt; {album.title}</p>
          <div className="mt-1 flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-extrabold text-slate-900">{album.title}</h1>
            <Badge variant={album.status === "PUBLISHED" ? "primary" : "neutral"} size="sm">
              {album.status}
            </Badge>
          </div>
          <p className="mt-2 text-sm text-slate-600">
            {orderedImages.length} {orderedImages.length === 1 ? "photo" : "photos"}
          </p>
        </div>
        {!isReadOnly && (
          <label className="inline-flex min-h-11 cursor-pointer items-center justify-center gap-2 rounded-lg bg-blue-900 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-950 focus-within:ring-2 focus-within:ring-blue-900 focus-within:ring-offset-2">
            <Upload size={16} aria-hidden="true" /> Upload Photos
            <input
              type="file"
              className="sr-only"
              accept="image/jpeg,image/png,image/webp"
              multiple
              onChange={(event) => {
                if (event.target.files) addFiles(event.target.files);
                event.currentTarget.value = "";
              }}
            />
          </label>
        )}
      </header>

      {errorMessage && (
        <div
          role="alert"
          className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800"
        >
          {errorMessage}
        </div>
      )}

      {!isReadOnly && (
        <Card className="space-y-4">
          <div>
            <h2 className="font-bold text-slate-900">Upload Photos</h2>
            <p className="mt-1 text-sm text-slate-600">
              JPEG, PNG, or WebP, up to 10 MB each. The first photo becomes the cover automatically
              unless you choose another.
            </p>
          </div>
          <label
            className="flex min-h-32 cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-slate-300 bg-slate-50 p-5 text-center text-sm text-slate-600 hover:border-blue-900 hover:bg-blue-50"
            onDragOver={(event) => event.preventDefault()}
            onDrop={(event) => {
              event.preventDefault();
              addFiles(event.dataTransfer.files);
            }}
          >
            <Upload size={24} className="mb-2 text-blue-900" aria-hidden="true" />
            <span className="font-semibold text-slate-800">Drag and drop photos here</span>
            <span className="mt-1">or choose photos</span>
            <input
              type="file"
              className="sr-only"
              accept="image/jpeg,image/png,image/webp"
              multiple
              onChange={(event) => event.target.files && addFiles(event.target.files)}
            />
          </label>
          {pendingImages.length > 0 && (
            <div className="space-y-3">
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {pendingImages.map((image) => (
                  <div key={image.id} className="flex gap-3 rounded-lg border border-slate-200 p-2">
                    <img
                      src={image.previewUrl}
                      alt="Selected upload preview"
                      className="size-14 rounded object-cover"
                    />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-slate-800">
                        {image.file.name}
                      </p>
                      <p className="text-xs text-emerald-700">Ready</p>
                    </div>
                    <button
                      type="button"
                      onClick={() => removePendingImage(image.id)}
                      className="rounded p-1 text-slate-500 hover:bg-slate-100 hover:text-red-700"
                      aria-label={`Remove ${image.file.name}`}
                    >
                      <X size={16} />
                    </button>
                  </div>
                ))}
              </div>
              <label className="flex items-center gap-2 text-sm text-slate-700">
                <input
                  type="checkbox"
                  checked={makeFirstUploadCover}
                  onChange={(event) => setMakeFirstUploadCover(event.target.checked)}
                />{" "}
                Upload / Change Cover: make the first uploaded photo the cover
              </label>
              <div className="flex flex-wrap gap-3">
                <Button
                  variant="primary"
                  onClick={() => uploadMutation.mutate(pendingImages)}
                  loading={uploadMutation.isPending}
                >
                  Upload {pendingImages.length} {pendingImages.length === 1 ? "Photo" : "Photos"}
                </Button>
                <Button
                  variant="outline"
                  onClick={clearPendingImages}
                  disabled={uploadMutation.isPending}
                >
                  Clear selection
                </Button>
              </div>
            </div>
          )}
        </Card>
      )}

      <section className="space-y-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Photos</h2>
          <p className="mt-1 text-sm text-slate-600">
            Drag a handle to reorder. Use the move buttons when using a keyboard.
          </p>
        </div>
        {orderedImages.length === 0 ? (
          <Card className="py-12 text-center">
            <ImageIcon className="mx-auto text-slate-400" size={36} />
            <h3 className="mt-3 font-bold text-slate-800">No photos in this album yet.</h3>
            <p className="mt-1 text-sm text-slate-500">Upload photos to get started.</p>
          </Card>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {orderedImages.map((image, index) => (
              <PhotoCard
                key={image.id}
                image={image}
                index={index}
                total={orderedImages.length}
                isCover={album.coverImageId === image.id}
                isBusy={
                  isReadOnly ||
                  setCoverMutation.isPending ||
                  deleteMutation.isPending ||
                  reorderMutation.isPending
                }
                onDragStart={() => setDraggedImageId(image.id)}
                onDrop={() => {
                  if (draggedImageId) moveImage(draggedImageId, index);
                  setDraggedImageId(null);
                }}
                onMove={(targetIndex) => moveImage(image.id, targetIndex)}
                onSetCover={() => setCoverMutation.mutate(image.id)}
                onDelete={() => setDeleteTarget(image)}
                onEdit={() => setEditingImage(image)}
              />
            ))}
          </div>
        )}
      </section>

      <ConfirmModal
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        title="Delete photo?"
        description="This photo will be permanently removed from the album."
        confirmText="Delete Photo"
        variant="danger"
        isLoading={deleteMutation.isPending}
      />
      <PhotoDetailsModal
        image={editingImage}
        isSaving={updateImageMutation.isPending}
        onClose={() => setEditingImage(null)}
        onSubmit={(event) => {
          event.preventDefault();
          const form = new FormData(event.currentTarget);
          if (editingImage)
            updateImageMutation.mutate({
              id: editingImage.id,
              altText: String(form.get("altText") ?? ""),
              caption: String(form.get("caption") ?? "") || null,
            });
        }}
      />
    </div>
  );
}

function PhotoCard({
  image,
  index,
  total,
  isCover,
  isBusy,
  onDragStart,
  onDrop,
  onMove,
  onSetCover,
  onDelete,
  onEdit,
}: {
  image: GalleryImageResponse;
  index: number;
  total: number;
  isCover: boolean;
  isBusy: boolean;
  onDragStart: () => void;
  onDrop: () => void;
  onMove: (targetIndex: number) => void;
  onSetCover: () => void;
  onDelete: () => void;
  onEdit: () => void;
}) {
  const [failed, setFailed] = useState(false);
  return (
    <Card
      className="overflow-hidden p-0"
      onDragOver={(event) => event.preventDefault()}
      onDrop={onDrop}
    >
      <div className="relative aspect-4/3 bg-slate-100">
        {image.thumbnailUrl && !failed ? (
          <img
            src={image.thumbnailUrl}
            alt={image.altText || "Gallery photo"}
            className="size-full object-cover"
            loading="lazy"
            onError={() => setFailed(true)}
          />
        ) : (
          <div className="flex size-full items-center justify-center text-slate-400">
            <ImageIcon size={32} />
          </div>
        )}
        {isCover && (
          <Badge variant="primary" size="sm" className="absolute left-2 top-2">
            <Star size={12} aria-hidden="true" /> Cover
          </Badge>
        )}
        <button
          type="button"
          draggable
          aria-label={`Drag ${image.altText || "photo"} to reorder`}
          onDragStart={onDragStart}
          className="absolute right-2 top-2 rounded bg-white/90 p-2 text-slate-700 shadow hover:bg-white"
        >
          <GripVertical size={16} />
        </button>
      </div>
      <div className="space-y-3 p-3">
        <p className="truncate text-sm font-medium text-slate-800">
          {image.caption || image.altText || "Untitled photo"}
        </p>
        <div className="grid grid-cols-2 gap-2">
          {!isCover && (
            <Button variant="outline" size="sm" disabled={isBusy} onClick={onSetCover}>
              Set as Cover
            </Button>
          )}
          <Button
            variant="outline"
            size="sm"
            disabled={isBusy || index === 0}
            onClick={() => onMove(index - 1)}
            aria-label={`Move ${image.altText || "photo"} earlier`}
          >
            Move earlier
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={isBusy || index === total - 1}
            onClick={() => onMove(index + 1)}
            aria-label={`Move ${image.altText || "photo"} later`}
          >
            Move later
          </Button>
          <Button variant="ghost" size="sm" disabled={isBusy} onClick={onEdit}>
            <Pencil size={14} aria-hidden="true" /> Edit text
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={isBusy}
            className="text-red-700 hover:bg-red-50"
            onClick={onDelete}
          >
            <Trash2 size={14} aria-hidden="true" /> Delete
          </Button>
        </div>
      </div>
    </Card>
  );
}

function PhotoDetailsModal({
  image,
  isSaving,
  onClose,
  onSubmit,
}: {
  image: GalleryImageResponse | null;
  isSaving: boolean;
  onClose: () => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
}) {
  if (!image) return null;
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="photo-details-title"
    >
      <form
        onSubmit={onSubmit}
        className="w-full max-w-md space-y-4 rounded-2xl bg-white p-6 shadow-2xl"
      >
        <h2 id="photo-details-title" className="text-lg font-bold text-slate-900">
          Edit photo details
        </h2>
        <Input
          label="Alt text"
          name="altText"
          defaultValue={image.altText}
          maxLength={300}
          required
        />
        <label className="block text-sm font-medium text-slate-700">
          Caption
          <textarea
            name="caption"
            defaultValue={image.caption ?? ""}
            rows={3}
            className="mt-1 block w-full rounded-lg border border-slate-300 p-2.5 text-sm"
          />
        </label>
        <div className="flex justify-end gap-2">
          <Button type="button" variant="outline" size="sm" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" size="sm" loading={isSaving}>
            Save details
          </Button>
        </div>
      </form>
    </div>
  );
}

function humanizeFilename(filename: string) {
  return (
    filename
      .replace(/\.[^.]+$/, "")
      .replace(/[-_]+/g, " ")
      .trim() || "Gallery photo"
  );
}

function messageFor(_error: unknown, fallback: string) {
  return fallback;
}
