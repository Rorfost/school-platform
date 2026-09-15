import { useQuery } from "@tanstack/react-query";
import { Calendar, Download, FileText, Newspaper, Pin } from "lucide-react";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { NoticeResponse, PageResponse } from "@/api/types";
import { Badge } from "@/components/ui/Badge";
import { EmptyState, ErrorState, LoadingState } from "@/components/common/StatusPanel";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { LABELS } from "@/utils/gujarati";

export function NoticesPage() {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: queryKeys.notices({ page: 0, size: 20 }),
    queryFn: () => apiRequest<PageResponse<NoticeResponse>>("/api/v1/public/notices?page=0&size=20"),
  });

  const notices = data?.items ?? [];

  return (
    <div className="space-y-6">
      <PageHeader
        title={LABELS.notices}
        description="શાળાની સત્તાવાર જાહેરાતો, રજાઓની યાદી અને અગત્યના પરિપત્રો"
        backTo="/"
        backLabel={LABELS.home}
      />

      {isLoading ? (
        <LoadingState message="સૂચનાઓ લોડ થઈ રહી છે..." />
      ) : error ? (
        <ErrorState onRetry={() => refetch()} />
      ) : notices.length === 0 ? (
        <EmptyState
          icon={Newspaper}
          title="હાલ કોઈ નવી સૂચના નથી."
          description="શાળા દ્વારા નવી સૂચના જાહેર થતાં જ અહીં જોવા મળશે."
        />
      ) : (
        <div className="space-y-4">
          {notices.map((item) => (
            <Card key={item.id} className="p-5 sm:p-6">
              <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
                <div className="flex items-start gap-3">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-900">
                    <FileText size={20} aria-hidden="true" />
                  </div>
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="text-base sm:text-lg font-bold text-slate-900">
                        {item.title}
                      </h2>
                      {item.isPinned && (
                        <Badge variant="accent" size="sm" className="gap-1">
                          <Pin size={11} aria-hidden="true" />
                          <span>મહત્વપૂર્ણ</span>
                        </Badge>
                      )}
                    </div>
                    {item.publishedAt && (
                      <div className="mt-1 flex items-center gap-1.5 text-xs text-slate-500">
                        <Calendar size={13} aria-hidden="true" />
                        <span>{item.publishedAt.slice(0, 10)}</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              <div className="mt-4 border-t border-slate-100 pt-4 text-sm text-slate-700 whitespace-pre-line leading-relaxed">
                {item.content}
              </div>

              {item.attachmentFilename && (
                <div className="mt-4 border-t border-slate-100 pt-3 flex items-center justify-between text-xs text-slate-500">
                  <span>જોડાણ: {item.attachmentFilename}</span>
                  <span className="font-semibold text-blue-900 inline-flex items-center gap-1">
                    <Download size={13} aria-hidden="true" />
                    <span>ડાઉનલોડ કરો</span>
                  </span>
                </div>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
