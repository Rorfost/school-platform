import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Download, FileDown, FolderOpen } from "lucide-react";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { DownloadResponse, PageResponse } from "@/api/types";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import {
  ContentSkeleton,
  EmptyState,
  ErrorState,
  LoadingState,
} from "@/components/common/StatusPanel";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { LABELS } from "@/utils/gujarati";

export function DownloadsPage() {
  const [selectedCategory, setSelectedCategory] = useState<string>("ALL");

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: queryKeys.downloads({ page: 0, size: 50 }),
    queryFn: () =>
      apiRequest<PageResponse<DownloadResponse>>("/api/v1/public/downloads?page=0&size=50"),
  });

  const downloads = data?.items ?? [];

  const categories = Array.from(
    new Set(downloads.map((d) => d.category).filter(Boolean) as string[]),
  );

  const filteredDownloads = downloads.filter((item) => {
    if (selectedCategory !== "ALL" && item.category !== selectedCategory) {
      return false;
    }
    return true;
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title={LABELS.downloads}
        description="શાળાના જરૂરી ફોર્મ, અરજી પત્રકો અને સત્તાવાર દસ્તાવેજો"
        backTo="/student"
        backLabel={LABELS.studentCorner}
      />

      {/* Category Pills Filter */}
      {categories.length > 0 && (
        <div className="flex flex-wrap items-center gap-2">
          <Button
            variant={selectedCategory === "ALL" ? "primary" : "outline"}
            size="sm"
            onClick={() => setSelectedCategory("ALL")}
          >
            તમામ ફાઈલો
          </Button>
          {categories.map((cat) => (
            <Button
              key={cat}
              variant={selectedCategory === cat ? "primary" : "outline"}
              size="sm"
              onClick={() => setSelectedCategory(cat)}
            >
              {cat}
            </Button>
          ))}
        </div>
      )}

      {isLoading ? (
        <>
          <LoadingState
            message="ડાઉનલોડ ફાઈલો લોડ થઈ રહી છે..."
            delayedMessage="થોડો સમય લાગી શકે છે."
          />
          <ContentSkeleton />
        </>
      ) : error ? (
        <ErrorState onRetry={() => refetch()} />
      ) : filteredDownloads.length === 0 ? (
        <EmptyState
          icon={FolderOpen}
          title="હાલ કોઈ ડાઉનલોડ ફાઈલ ઉપલબ્ધ નથી."
          description={
            selectedCategory !== "ALL"
              ? "પસંદ કરેલ કેટેગરીમાં ફાઈલો ઉપલબ્ધ નથી."
              : "જરૂરી ફોર્મ્સ અને પત્રકો ટૂંક સમયમાં અહીં ઉપલબ્ધ કરવામાં આવશે."
          }
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {filteredDownloads.map((item) => (
            <Card
              key={item.id}
              className="p-5 flex flex-col justify-between hover:border-blue-200 transition-colors shadow-xs"
            >
              <div className="flex items-start gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-900">
                  <FileDown size={22} aria-hidden="true" />
                </div>
                <div className="flex-1">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <h3 className="font-bold text-slate-900 text-base">{item.title}</h3>
                    {item.category && (
                      <Badge variant="neutral" size="sm">
                        {item.category}
                      </Badge>
                    )}
                  </div>
                  {item.description && (
                    <p className="mt-1 text-xs sm:text-sm text-slate-600 leading-relaxed">
                      {item.description}
                    </p>
                  )}
                </div>
              </div>
              <div className="mt-4 flex flex-col gap-2 border-t border-slate-100 pt-3 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between">
                <span className="min-w-0 truncate">{item.filename}</span>
                {item.url ? (
                  <a
                    href={item.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex min-h-11 shrink-0 items-center justify-center gap-1.5 rounded-md bg-blue-50 px-3 py-1.5 font-semibold text-blue-900 transition-colors hover:bg-blue-100 hover:underline"
                  >
                    <Download size={14} aria-hidden="true" />
                    <span>ડાઉનલોડ કરો</span>
                  </a>
                ) : (
                  <span className="font-medium text-slate-400">ડાઉનલોડ</span>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
