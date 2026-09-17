import { useState } from "react";
import { Camera, Image as ImageIcon, ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import { usePublicGalleryAlbums } from "@/features/public/usePublicContent";
import { CardGridSkeleton, EmptyState, ErrorState, LoadingState } from "@/components/common/StatusPanel";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { LABELS } from "@/utils/gujarati";

export function GalleryPage() {
  const { data, isLoading, error, refetch } = usePublicGalleryAlbums(0, 50);
  const [failedCoverIds, setFailedCoverIds] = useState<Set<string>>(() => new Set());

  const albums = data?.items ?? [];

  return (
    <div className="space-y-6">
      <PageHeader
        title={LABELS.gallery}
        description="શાળાની વિવિધ શૈક્ષણિક, સાંસ્કૃતિક અને રમતગમત પ્રવૃત્તિઓની યાદગાર ક્ષણો"
        backTo="/"
        backLabel={LABELS.home}
      />

      {isLoading ? (
        <>
          <LoadingState
            message="ફોટો ગેલેરી લોડ થઈ રહી છે..."
            delayedMessage="થોડો સમય લાગી શકે છે."
          />
          <CardGridSkeleton />
        </>
      ) : error ? (
        <ErrorState onRetry={() => refetch()} />
      ) : albums.length === 0 ? (
        <EmptyState
          icon={Camera}
          title="હાલ કોઈ ફોટો આલ્બમ ઉપલબ્ધ નથી."
          description="શાળાના કાર્યક્રમો અને ઉત્સવોના ફોટો આલ્બમ ટૂંક સમયમાં અહીં ઉમેરવામાં આવશે."
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
          {albums.map((album) => (
            <Link
              key={album.id}
              to={`/gallery/${album.id}`}
              className="group focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-900 rounded-xl"
            >
              <Card
                variant="interactive"
                className="overflow-hidden p-0 h-full flex flex-col justify-between"
              >
                <div>
                  <div className="aspect-video bg-gradient-to-br from-blue-50 to-slate-100 flex items-center justify-center overflow-hidden text-blue-900/60 group-hover:text-blue-900 transition-colors">
                    {album.coverImageThumbnailUrl && !failedCoverIds.has(album.id) ? (
                      <img
                        src={album.coverImageThumbnailUrl}
                        alt={album.title}
                        className="size-full object-cover transition-transform duration-300 group-hover:scale-105"
                        loading="lazy"
                        decoding="async"
                        onError={() =>
                          setFailedCoverIds((current) => new Set(current).add(album.id))
                        }
                      />
                    ) : (
                      <ImageIcon size={40} aria-hidden="true" />
                    )}
                  </div>
                  <div className="p-4 sm:p-5">
                    <h3 className="font-bold text-slate-900 text-base group-hover:text-blue-900 transition-colors">
                      {album.title}
                    </h3>
                    <p className="mt-1 text-xs font-medium text-slate-500">
                      {album.imageCount} ફોટા
                    </p>
                    {album.description && (
                      <p className="mt-1.5 text-xs sm:text-sm text-slate-600 line-clamp-2 leading-relaxed">
                        {album.description}
                      </p>
                    )}
                  </div>
                </div>
                <div className="px-4 pb-4 sm:px-5 sm:pb-5 pt-0 flex items-center justify-between text-xs font-semibold text-blue-900">
                  <span>ફોટાઓ જુઓ</span>
                  <ArrowRight
                    size={14}
                    className="transition-transform group-hover:translate-x-1"
                    aria-hidden="true"
                  />
                </div>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
