import { useState } from "react";
import { Camera, Image as ImageIcon, X, ZoomIn } from "lucide-react";
import { useParams } from "react-router-dom";
import { usePublicGalleryImages } from "@/features/public/usePublicContent";
import type { GalleryImageResponse } from "@/api/types";
import { CardGridSkeleton, EmptyState, ErrorState, LoadingState } from "@/components/common/StatusPanel";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { LABELS } from "@/utils/gujarati";

export function GalleryAlbumPage() {
  const { albumId } = useParams<{ albumId: string }>();
  const { data: images, isLoading, error, refetch } = usePublicGalleryImages(albumId);
  const [selectedImage, setSelectedImage] = useState<GalleryImageResponse | null>(null);
  const [failedImageIds, setFailedImageIds] = useState<Set<string>>(() => new Set());

  const markImageFailed = (imageId: string) => {
    setFailedImageIds((current) => new Set(current).add(imageId));
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title={LABELS.gallery}
        description="આલ્બમના તસવીરો"
        backTo="/gallery"
        backLabel={LABELS.gallery}
      />

      {isLoading ? (
        <>
          <LoadingState
            message="તસવીરો લોડ થઈ રહી છે..."
            delayedMessage="થોડો સમય લાગી શકે છે."
          />
          <CardGridSkeleton cards={9} />
        </>
      ) : error ? (
        <ErrorState onRetry={() => refetch()} />
      ) : !images || images.length === 0 ? (
        <EmptyState
          icon={Camera}
          title="આ આલ્બમમાં તસવીરો ઉપલબ્ધ નથી."
          description="ટૂંક સમયમાં નવી તસવીરો અહીં ઉમેરવામાં આવશે."
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {images.map((img) => (
            <Card
              key={img.id}
              variant="interactive"
              className="group overflow-hidden p-0 cursor-pointer focus-visible:ring-2 focus-visible:ring-blue-900"
              onClick={() => setSelectedImage(img)}
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") {
                  e.preventDefault();
                  setSelectedImage(img);
                }
              }}
            >
              <div className="relative aspect-4/3 bg-slate-100 overflow-hidden">
                {img.url && !failedImageIds.has(img.id) ? (
                  <img
                    src={img.thumbnailUrl || img.url}
                    alt={img.altText || "ગેલેરી તસવીર"}
                    className="size-full object-cover transition-transform duration-300 group-hover:scale-105"
                    loading="lazy"
                    decoding="async"
                    onError={() => markImageFailed(img.id)}
                  />
                ) : (
                  <div className="size-full flex items-center justify-center text-slate-400">
                    <ImageIcon size={32} aria-hidden="true" />
                  </div>
                )}
                <div className="absolute inset-0 bg-slate-900/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white">
                  <ZoomIn size={24} aria-hidden="true" />
                </div>
              </div>
              {(img.altText || img.caption) && (
                <div className="p-3 text-xs text-slate-700 font-medium truncate">
                  {img.caption || img.altText}
                </div>
              )}
            </Card>
          ))}
        </div>
      )}

      {/* Image Lightbox Modal */}
      {selectedImage && (
        <div
          className="fixed inset-0 z-50 bg-slate-900/80 backdrop-blur-xs flex items-center justify-center p-4"
          onClick={() => setSelectedImage(null)}
          role="dialog"
          aria-modal="true"
          aria-label="તસવીર મોટી જુઓ"
        >
          <div
            className="relative max-w-4xl w-full bg-white rounded-2xl overflow-hidden shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3">
              <span className="text-sm font-semibold text-slate-900">
                {selectedImage.caption || selectedImage.altText || "તસવીર"}
              </span>
              <button
                type="button"
                onClick={() => setSelectedImage(null)}
                aria-label="બંધ કરો"
                className="flex size-9 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-900"
              >
                <X size={20} aria-hidden="true" />
              </button>
            </div>
            <div className="p-4 bg-slate-950 flex items-center justify-center max-h-[75vh]">
              {selectedImage.url ? (
                <img
                  src={selectedImage.url}
                  alt={selectedImage.altText || "મોટી ગેલેરી તસવીર"}
                  className="max-h-[70vh] w-auto max-w-full object-contain rounded-lg"
                />
              ) : (
                <div className="p-12 text-slate-400 text-center">
                  <ImageIcon size={48} className="mx-auto mb-2" />
                  <p>તસવીર ઉપલબ્ધ નથી</p>
                </div>
              )}
            </div>
            {selectedImage.caption && (
              <div className="p-4 border-t border-slate-100 text-sm text-slate-700 text-center">
                {selectedImage.caption}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
