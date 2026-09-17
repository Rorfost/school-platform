import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Image as ImageIcon, Pencil, Plus, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type {
  GalleryAlbumRequest,
  GalleryAlbumResponse,
  GalleryAlbumUpdateRequest,
  PageResponse,
} from "@/api/types";
import { LoadingState } from "@/components/common/StatusPanel";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ConfirmModal } from "@/components/ui/ConfirmModal";
import { Input } from "@/components/ui/Input";

export function AdminGalleryPage() {
  const queryClient = useQueryClient();
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingAlbum, setEditingAlbum] = useState<GalleryAlbumResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<GalleryAlbumResponse | null>(null);
  const [publicationTarget, setPublicationTarget] = useState<GalleryAlbumResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const { data, isLoading, error, refetch } = useQuery<PageResponse<GalleryAlbumResponse>>({
    queryKey: queryKeys.adminGalleryAlbums({ page: 0, size: 50 }),
    queryFn: () =>
      apiRequest<PageResponse<GalleryAlbumResponse>>("/api/v1/admin/gallery/albums?page=0&size=50"),
  });
  const albums = data?.items ?? [];

  const refreshGallery = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.adminGalleryAlbums() });
    queryClient.invalidateQueries({ queryKey: queryKeys.galleryAlbums() });
  };

  const createMutation = useMutation({
    mutationFn: (payload: GalleryAlbumRequest) =>
      apiRequest<GalleryAlbumResponse>("/api/v1/admin/gallery/albums", {
        method: "POST",
        body: payload,
      }),
    onSuccess: () => {
      refreshGallery();
      setIsCreateOpen(false);
      setErrorMessage(null);
    },
    onError: (error) => setErrorMessage(messageFor(error, "Could not create album.")),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: GalleryAlbumUpdateRequest }) =>
      apiRequest<GalleryAlbumResponse>(`/api/v1/admin/gallery/albums/${id}`, {
        method: "PUT",
        body: payload,
      }),
    onSuccess: () => {
      refreshGallery();
      setEditingAlbum(null);
      setErrorMessage(null);
    },
    onError: (error) => setErrorMessage(messageFor(error, "Could not update album.")),
  });

  const publicationMutation = useMutation({
    mutationFn: (album: GalleryAlbumResponse) =>
      apiRequest<void>(
        `/api/v1/admin/gallery/albums/${album.id}/${album.status === "PUBLISHED" ? "unpublish" : "publish"}`,
        { method: "POST" },
      ),
    onSuccess: () => {
      refreshGallery();
      setPublicationTarget(null);
    },
    onError: (error) => setErrorMessage(messageFor(error, "Could not update album visibility.")),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) =>
      apiRequest<void>(`/api/v1/admin/gallery/albums/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      refreshGallery();
      setDeleteTarget(null);
    },
    onError: (error) => setErrorMessage(messageFor(error, "Could not delete album.")),
  });

  const submitAlbum = (event: React.FormEvent<HTMLFormElement>, target?: GalleryAlbumResponse) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const payload = {
      title: String(form.get("title") ?? ""),
      description: String(form.get("description") ?? "") || null,
    };
    if (target) {
      updateMutation.mutate({ id: target.id, payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  if (isLoading) return <LoadingState message="Loading gallery albums..." />;

  if (error) {
    return (
      <div className="rounded-xl border border-red-200 bg-red-50 p-5 text-sm text-red-800">
        Could not load gallery.{" "}
        <button onClick={() => refetch()} className="font-semibold underline">
          Try again.
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-6xl space-y-6">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Photo Gallery Management</h1>
          <p className="mt-1 text-sm text-slate-600">
            Create albums, arrange event photos, and choose what appears publicly.
          </p>
        </div>
        <Button variant="primary" onClick={() => setIsCreateOpen(true)} className="shrink-0">
          <Plus size={16} aria-hidden="true" /> New Album
        </Button>
      </header>

      {errorMessage && (
        <div
          role="alert"
          className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800"
        >
          {errorMessage}
        </div>
      )}

      {albums.length === 0 ? (
        <Card className="py-12 text-center">
          <ImageIcon className="mx-auto text-slate-400" size={36} aria-hidden="true" />
          <h2 className="mt-3 font-bold text-slate-800">No albums yet.</h2>
          <p className="mt-1 text-sm text-slate-500">
            Create your first album to start adding school photos.
          </p>
        </Card>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {albums.map((album) => (
            <Card key={album.id} className="overflow-hidden p-0">
              <AlbumCover album={album} />
              <div className="space-y-4 p-5">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h2 className="font-bold text-slate-900">{album.title}</h2>
                    <p className="mt-1 text-xs font-medium text-slate-500">
                      {album.imageCount} {album.imageCount === 1 ? "photo" : "photos"}
                    </p>
                  </div>
                  <StatusBadge status={album.status} />
                </div>
                {album.description && (
                  <p className="line-clamp-2 text-sm leading-relaxed text-slate-600">
                    {album.description}
                  </p>
                )}
                <div className="grid grid-cols-2 gap-2 border-t border-slate-100 pt-4">
                  <Link
                    to={`/admin/gallery/${album.id}`}
                    className="inline-flex min-h-9 items-center justify-center rounded-md bg-blue-900 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-950 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-900 focus-visible:ring-offset-2"
                  >
                    Manage Photos
                  </Link>
                  {album.status !== "ARCHIVED" && (
                    <Button variant="outline" size="sm" onClick={() => setEditingAlbum(album)}>
                      <Pencil size={13} aria-hidden="true" /> Edit Album
                    </Button>
                  )}
                  {album.status !== "ARCHIVED" && (
                    <Button variant="outline" size="sm" onClick={() => setPublicationTarget(album)}>
                      {album.status === "PUBLISHED" ? "Unpublish" : "Publish"}
                    </Button>
                  )}
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-red-700 hover:bg-red-50"
                    onClick={() => setDeleteTarget(album)}
                  >
                    <Trash2 size={14} aria-hidden="true" /> Delete Album
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      <AlbumFormModal
        isOpen={isCreateOpen || Boolean(editingAlbum)}
        album={editingAlbum}
        isSaving={createMutation.isPending || updateMutation.isPending}
        onClose={() => {
          setIsCreateOpen(false);
          setEditingAlbum(null);
          setErrorMessage(null);
        }}
        onSubmit={submitAlbum}
      />
      <ConfirmModal
        isOpen={Boolean(publicationTarget)}
        onClose={() => setPublicationTarget(null)}
        onConfirm={() => publicationTarget && publicationMutation.mutate(publicationTarget)}
        title={publicationTarget?.status === "PUBLISHED" ? "Unpublish album?" : "Publish album?"}
        description={
          publicationTarget?.status === "PUBLISHED"
            ? "This album and its photos will no longer appear in the public gallery."
            : "This album and its photos will become visible in the public gallery."
        }
        confirmText={
          publicationTarget?.status === "PUBLISHED" ? "Unpublish Album" : "Publish Album"
        }
        variant="warning"
        isLoading={publicationMutation.isPending}
      />
      <ConfirmModal
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        title="Delete album?"
        description="This album and its photos will be permanently removed."
        confirmText="Delete Album"
        variant="danger"
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}

function AlbumCover({ album }: { album: GalleryAlbumResponse }) {
  const [failed, setFailed] = useState(false);
  return (
    <div className="aspect-video bg-slate-100">
      {album.coverImageThumbnailUrl && !failed ? (
        <img
          src={album.coverImageThumbnailUrl}
          alt={`Cover for ${album.title}`}
          className="size-full object-cover"
          loading="lazy"
          onError={() => setFailed(true)}
        />
      ) : (
        <div className="flex size-full items-center justify-center text-slate-400">
          <ImageIcon size={38} aria-hidden="true" />
        </div>
      )}
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  return status === "PUBLISHED" ? (
    <Badge variant="primary" size="sm">
      <CheckCircle2 size={12} aria-hidden="true" /> PUBLISHED
    </Badge>
  ) : (
    <Badge variant="neutral" size="sm">
      {status}
    </Badge>
  );
}

function AlbumFormModal({
  isOpen,
  album,
  isSaving,
  onClose,
  onSubmit,
}: {
  isOpen: boolean;
  album: GalleryAlbumResponse | null;
  isSaving: boolean;
  onClose: () => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>, target?: GalleryAlbumResponse) => void;
}) {
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
      <div className="w-full max-w-md space-y-4 rounded-2xl bg-white p-6 shadow-2xl">
        <h2 className="text-lg font-bold text-slate-900">
          {album ? "Edit Album" : "Create Album"}
        </h2>
        <form className="space-y-4" onSubmit={(event) => onSubmit(event, album ?? undefined)}>
          <Input
            label="Album Name"
            name="title"
            defaultValue={album?.title}
            required
            maxLength={160}
            placeholder="e.g. Sports Day 2026"
          />
          <label className="block text-sm font-medium text-slate-700">
            Description
            <textarea
              name="description"
              defaultValue={album?.description ?? ""}
              rows={4}
              className="mt-1 block w-full rounded-lg border border-slate-300 p-2.5 text-sm"
              placeholder="Optional event details"
            />
          </label>
          <p className="text-xs text-slate-500">
            Photos are optional. Add and arrange them after creating the album.
          </p>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" size="sm" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" size="sm" loading={isSaving}>
              {album ? "Save Changes" : "Create Album"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

function messageFor(_error: unknown, fallback: string) {
  return fallback;
}
