import { useEffect, useState } from "react";
import {
    Camera,
    ChevronLeft,
    ChevronRight,
    Image as ImageIcon,
    Loader2,
    X,
    ZoomIn,
} from "lucide-react";
import { useParams } from "react-router-dom";
import { usePublicGalleryImagesPaginated } from "@/features/public/usePublicContent";
import type { GalleryImageResponse } from "@/api/types";
import {
    CardGridSkeleton,
    EmptyState,
    ErrorState,
    LoadingState,
} from "@/components/common/StatusPanel";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { LABELS } from "@/utils/gujarati";
import { ResponsiveMedia } from "@/components/common/ResponsiveMedia";

export function GalleryAlbumPage() {
    const { albumId } = useParams<{ albumId: string }>();
    const [page, setPage] = useState(0);
    const pageSize = 24;
    const {
        data: pageData,
        isLoading,
        isFetching,
        error,
        refetch,
    } = usePublicGalleryImagesPaginated(albumId, page, pageSize);

    // Accumulated images across pages with deduplication by ID
    const [accumulatedImages, setAccumulatedImages] = useState<GalleryImageResponse[]>([]);
    const [selectedIndex, setSelectedIndex] = useState<number | null>(null);
    const selectedImage =
        selectedIndex !== null ? (accumulatedImages[selectedIndex] ?? null) : null;

    const handlePrevImage = () => {
        if (selectedIndex !== null && selectedIndex > 0) {
            setSelectedIndex(selectedIndex - 1);
        }
    };

    const handleNextImage = () => {
        if (selectedIndex !== null && selectedIndex < accumulatedImages.length - 1) {
            setSelectedIndex(selectedIndex + 1);
        }
    };

    useEffect(() => {
        // Reset when albumId changes
        setPage(0);
        setAccumulatedImages([]);
        setSelectedIndex(null);
    }, [albumId]);

    useEffect(() => {
        if (!pageData?.items) return;
        setAccumulatedImages((prev) => {
            if (page === 0) return pageData.items;
            const existingIds = new Set(prev.map((img) => img.id));
            const newItems = pageData.items.filter((img) => !existingIds.has(img.id));
            return [...prev, ...newItems];
        });
    }, [pageData, page]);

    const hasMore = pageData ? page < pageData.totalPages - 1 : false;

    const handleLoadMore = () => {
        if (hasMore && !isFetching) {
            setPage((prev) => prev + 1);
        }
    };

    useEffect(() => {
        if (selectedIndex === null) return;
        const originalOverflow = document.body.style.overflow;
        document.body.style.overflow = "hidden";

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                setSelectedIndex(null);
            } else if (event.key === "ArrowLeft") {
                setSelectedIndex((prev) => (prev !== null && prev > 0 ? prev - 1 : prev));
            } else if (event.key === "ArrowRight") {
                setSelectedIndex((prev) =>
                    prev !== null && prev < accumulatedImages.length - 1 ? prev + 1 : prev,
                );
            }
        };

        window.addEventListener("keydown", handleKeyDown);
        return () => {
            document.body.style.overflow = originalOverflow;
            window.removeEventListener("keydown", handleKeyDown);
        };
    }, [selectedIndex, accumulatedImages.length]);

    return (
        <div className="space-y-6">
            <PageHeader
                title={LABELS.gallery}
                description="આલ્બમના તસવીરો"
                backTo="/gallery"
                backLabel={LABELS.gallery}
            />

            {isLoading && page === 0 ? (
                <>
                    <LoadingState
                        message="તસવીરો લોડ થઈ રહી છે..."
                        delayedMessage="થોડો સમય લાગી શકે છે."
                    />
                    <CardGridSkeleton cards={9} />
                </>
            ) : error && accumulatedImages.length === 0 ? (
                <ErrorState onRetry={() => refetch()} />
            ) : accumulatedImages.length === 0 ? (
                <EmptyState
                    icon={Camera}
                    title="આ આલ્બમમાં તસવીરો ઉપલબ્ધ નથી."
                    description="ટૂંક સમયમાં નવી તસવીરો અહીં ઉમેરવામાં આવશે."
                />
            ) : (
                <>
                    <div className="grid grid-cols-1 gap-3 min-[360px]:grid-cols-2 sm:gap-4 md:grid-cols-3 lg:grid-cols-4">
                        {accumulatedImages.map((img, index) => (
                            <Card
                                key={img.id}
                                variant="interactive"
                                className="group overflow-hidden p-0 cursor-pointer focus-visible:ring-2 focus-visible:ring-brand-900"
                                role="button"
                                aria-label={`${img.caption || img.altText || "ગેલેરી તસવીર"} મોટી જુઓ`}
                                onClick={() => setSelectedIndex(index)}
                                tabIndex={0}
                                onKeyDown={(e) => {
                                    if (e.key === "Enter" || e.key === " ") {
                                        e.preventDefault();
                                        setSelectedIndex(index);
                                    }
                                }}
                            >
                                <div className="relative aspect-4/3 bg-slate-100 overflow-hidden">
                                    <ResponsiveMedia
                                        src={img.thumbnailUrl || img.url}
                                        alt={img.altText || "ગેલેરી તસવીર"}
                                        transformOptions={{
                                            widths: [320, 480, 640],
                                            defaultWidth: 480,
                                            quality: 80,
                                            cropMode: "c-at_max",
                                            aspectRatio: "4-3",
                                        }}
                                        sizes="(max-width: 480px) 100vw, (max-width: 768px) 50vw, (max-width: 1024px) 33vw, 25vw"
                                        className="size-full object-cover transition-transform duration-300 group-hover:scale-105"
                                        loading="lazy"
                                        decoding="async"
                                        fallback={
                                            <div className="size-full flex items-center justify-center text-slate-400">
                                                <ImageIcon size={32} aria-hidden="true" />
                                            </div>
                                        }
                                    />
                                    <div className="absolute inset-0 bg-slate-900/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white pointer-events-none">
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

                    {/* Pagination / Load More Controls */}
                    {hasMore && (
                        <div className="flex justify-center pt-4">
                            <button
                                type="button"
                                onClick={handleLoadMore}
                                disabled={isFetching}
                                className="inline-flex items-center gap-2 px-6 py-2.5 rounded-xl bg-blue-900 text-white font-medium hover:bg-blue-800 disabled:opacity-60 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-900 focus-visible:ring-offset-2"
                                aria-label="વધુ તસવીરો લોડ કરો"
                            >
                                {isFetching ? (
                                    <>
                                        <Loader2
                                            size={18}
                                            className="animate-spin"
                                            aria-hidden="true"
                                        />
                                        <span>લોડ થઈ રહ્યું છે...</span>
                                    </>
                                ) : (
                                    <span>વધુ તસવીરો જુઓ</span>
                                )}
                            </button>
                        </div>
                    )}
                </>
            )}

            {/* Image Lightbox Modal */}
            {selectedImage && (
                <div
                    className="fixed inset-0 z-50 bg-slate-900/80 backdrop-blur-sm flex items-center justify-center p-4"
                    onClick={() => setSelectedIndex(null)}
                    role="dialog"
                    aria-modal="true"
                    aria-label="તસવીર મોટી જુઓ"
                >
                    <div
                        className="relative max-w-4xl w-full bg-white rounded-2xl overflow-hidden shadow-2xl"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3">
                            <span className="text-sm font-semibold text-slate-900 truncate pr-2">
                                {selectedImage.caption || selectedImage.altText || "તસવીર"}
                            </span>
                            <div className="flex items-center gap-1">
                                <span className="text-xs text-slate-500 font-medium px-2">
                                    {(selectedIndex ?? 0) + 1} / {accumulatedImages.length}
                                </span>
                                <button
                                    type="button"
                                    onClick={() => setSelectedIndex(null)}
                                    aria-label="બંધ કરો"
                                    className="flex size-11 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-900"
                                >
                                    <X size={20} aria-hidden="true" />
                                </button>
                            </div>
                        </div>
                        <div className="relative flex max-h-[calc(100dvh-7rem)] items-center justify-center bg-slate-950 p-3 sm:p-4">
                            {/* Prev Button */}
                            {selectedIndex !== null && selectedIndex > 0 && (
                                <button
                                    type="button"
                                    onClick={handlePrevImage}
                                    aria-label="પાછલી તસવીર"
                                    className="absolute left-3 z-10 flex min-h-[44px] min-w-[44px] size-11 items-center justify-center rounded-full bg-black/60 text-white hover:bg-black/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white transition-colors"
                                >
                                    <ChevronLeft size={24} aria-hidden="true" />
                                </button>
                            )}

                            <ResponsiveMedia
                                src={selectedImage.url}
                                alt={selectedImage.altText || "મોટી ગેલેરી તસવીર"}
                                transformOptions={{
                                    widths: [640, 960, 1280, 1600],
                                    defaultWidth: 1280,
                                    quality: 85,
                                    cropMode: "c-at_max",
                                }}
                                sizes="(max-width: 1024px) 100vw, 1280px"
                                className="max-h-[calc(100dvh-9rem)] w-auto max-w-full object-contain rounded-lg"
                                loading="eager"
                                decoding="async"
                                fallback={
                                    <div className="p-12 text-slate-400 text-center">
                                        <ImageIcon size={48} className="mx-auto mb-2" />
                                        <p>તસવીર ઉપલબ્ધ નથી</p>
                                    </div>
                                }
                            />

                            {/* Next Button */}
                            {selectedIndex !== null &&
                                selectedIndex < accumulatedImages.length - 1 && (
                                    <button
                                        type="button"
                                        onClick={handleNextImage}
                                        aria-label="આગળની તસવીર"
                                        className="absolute right-3 z-10 flex min-h-[44px] min-w-[44px] size-11 items-center justify-center rounded-full bg-black/60 text-white hover:bg-black/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white transition-colors"
                                    >
                                        <ChevronRight size={24} aria-hidden="true" />
                                    </button>
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
