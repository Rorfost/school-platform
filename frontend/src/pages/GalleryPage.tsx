import { useQuery } from "@tanstack/react-query";
import { Camera, Image as ImageIcon } from "lucide-react";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { GalleryAlbumResponse, PageResponse } from "@/api/types";
import { EmptyState, ErrorState, LoadingState } from "@/components/common/StatusPanel";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { LABELS } from "@/utils/gujarati";

export function GalleryPage() {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: queryKeys.galleryAlbums({ page: 0, size: 20 }),
    queryFn: () => apiRequest<PageResponse<GalleryAlbumResponse>>("/api/v1/public/gallery/albums?page=0&size=20"),
  });

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
        <LoadingState message="ફોટો ગેલેરી લોડ થઈ રહી છે..." />
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
            <Card key={album.id} variant="interactive" className="overflow-hidden p-0">
              <div className="aspect-video bg-slate-100 flex items-center justify-center text-slate-400">
                <ImageIcon size={32} aria-hidden="true" />
              </div>
              <div className="p-4">
                <h3 className="font-bold text-slate-900 text-base">{album.title}</h3>
                {album.description && (
                  <p className="mt-1 text-xs sm:text-sm text-slate-600 line-clamp-2">
                    {album.description}
                  </p>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
