import { useQuery } from "@tanstack/react-query";
import { Download, FileDown, FolderOpen } from "lucide-react";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { DownloadResponse, PageResponse } from "@/api/types";
import { EmptyState, ErrorState, LoadingState } from "@/components/common/StatusPanel";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { LABELS } from "@/utils/gujarati";

export function DownloadsPage() {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: queryKeys.downloads({ page: 0, size: 20 }),
    queryFn: () => apiRequest<PageResponse<DownloadResponse>>("/api/v1/public/downloads?page=0&size=20"),
  });

  const downloads = data?.items ?? [];

  return (
    <div className="space-y-6">
      <PageHeader
        title={LABELS.downloads}
        description="શાળાના જરૂરી ફોર્મ, અરજી પત્રકો અને સત્તાવાર દસ્તાવેજો"
        backTo="/student"
        backLabel={LABELS.studentCorner}
      />

      {isLoading ? (
        <LoadingState message="ડાઉનલોડ ફાઈલો લોડ થઈ રહી છે..." />
      ) : error ? (
        <ErrorState onRetry={() => refetch()} />
      ) : downloads.length === 0 ? (
        <EmptyState
          icon={FolderOpen}
          title="હાલ કોઈ ડાઉનલોડ ફાઈલ ઉપલબ્ધ નથી."
          description="જરૂરી ફોર્મ્સ અને પત્રકો ટૂંક સમયમાં અહીં ઉપલબ્ધ કરવામાં આવશે."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {downloads.map((item) => (
            <Card key={item.id} className="p-5 flex flex-col justify-between">
              <div className="flex items-start gap-3">
                <FileDown className="text-blue-900 shrink-0 mt-0.5" size={22} aria-hidden="true" />
                <div>
                  <h3 className="font-semibold text-slate-900">{item.title}</h3>
                  {item.category && (
                    <span className="inline-block mt-1 text-xs font-medium text-slate-500 bg-slate-100 px-2 py-0.5 rounded">
                      {item.category}
                    </span>
                  )}
                </div>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
                <span>{item.filename}</span>
                {item.url ? (
                  <a
                    href={item.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="font-medium text-blue-900 hover:underline flex items-center gap-1"
                  >
                    <Download size={13} aria-hidden="true" />
                    <span>ડાઉનલોડ</span>
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
