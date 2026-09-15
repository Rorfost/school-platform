import { Calendar, Download, FileText, Newspaper, Pin } from "lucide-react";
import { useParams } from "react-router-dom";
import { usePublicNotice } from "@/features/public/usePublicContent";
import { Badge } from "@/components/ui/Badge";
import { EmptyState, LoadingState } from "@/components/common/StatusPanel";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { LABELS } from "@/utils/gujarati";

export function NoticeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: notice, isLoading } = usePublicNotice(id);

  if (isLoading) {
    return (
      <div className="space-y-6 max-w-3xl">
        <PageHeader
          title={LABELS.notices}
          backTo="/notices"
          backLabel={LABELS.notices}
        />
        <LoadingState message="સૂચના વિગત લોડ થઈ રહી છે..." />
      </div>
    );
  }

  if (!notice) {
    return (
      <div className="space-y-6 max-w-3xl">
        <PageHeader
          title={LABELS.notices}
          backTo="/notices"
          backLabel={LABELS.notices}
        />
        <EmptyState
          icon={Newspaper}
          title="સૂચના મળી નથી."
          description="તમે શોધેલી સૂચના ઉપલબ્ધ નથી અથવા દૂર કરવામાં આવી છે."
        />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <PageHeader
        title={notice.title}
        description="સૂચના વિગત"
        backTo="/notices"
        backLabel={LABELS.notices}
      />

      <Card className="p-6 sm:p-8 space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
          <div className="flex items-center gap-2">
            <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-900">
              <FileText size={20} aria-hidden="true" />
            </div>
            {notice.pinned && (
              <Badge variant="accent" size="sm" className="gap-1">
                <Pin size={11} aria-hidden="true" />
                <span>મહત્વપૂર્ણ</span>
              </Badge>
            )}
          </div>
          {notice.expiresAt && (
            <div className="flex items-center gap-1.5 text-xs text-slate-500 bg-slate-50 px-3 py-1.5 rounded-md border border-slate-200">
              <Calendar size={13} aria-hidden="true" />
              <span>મુદત તારીખ: {notice.expiresAt.slice(0, 10)}</span>
            </div>
          )}
        </div>

        <div className="text-base text-slate-800 whitespace-pre-line leading-relaxed">
          {notice.body}
        </div>

        {notice.attachmentFilename && notice.attachmentUrl && (
          <div className="border-t border-slate-100 pt-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-blue-50/50 p-4 rounded-xl border">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                સામેલ ફાઈલ / જોડાણ
              </p>
              <p className="text-sm font-semibold text-slate-900 mt-0.5">
                {notice.attachmentFilename}
              </p>
            </div>
            <a
              href={notice.attachmentUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center justify-center gap-2 px-4 py-2 rounded-lg bg-blue-900 text-white text-sm font-medium hover:bg-blue-950 transition-colors shrink-0"
            >
              <Download size={16} aria-hidden="true" />
              <span>ફાઈલ ડાઉનલોડ કરો</span>
            </a>
          </div>
        )}
      </Card>
    </div>
  );
}
