import { useState } from "react";
import { Calendar, Download, Eye, FileText, Newspaper, Pin, Search } from "lucide-react";
import { Link } from "react-router-dom";
import { usePublicNotices } from "@/features/public/usePublicContent";
import { Badge } from "@/components/ui/Badge";
import { EmptyState, ErrorState, LoadingState } from "@/components/common/StatusPanel";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { LABELS } from "@/utils/gujarati";

export function NoticesPage() {
  const [searchTerm, setSearchTerm] = useState("");
  const { data, isLoading, error, refetch } = usePublicNotices(0, 50);

  const notices = data?.items ?? [];

  const filteredNotices = notices.filter(
    (n) =>
      n.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      n.body.toLowerCase().includes(searchTerm.toLowerCase()),
  );

  return (
    <div className="space-y-6">
      <PageHeader
        title={LABELS.notices}
        description="શાળાની સત્તાવાર જાહેરાતો, રજાઓની યાદી અને અગત્યના પરિપત્રો"
        backTo="/"
        backLabel={LABELS.home}
      />

      <div className="max-w-md">
        <Input
          type="search"
          placeholder="સૂચનાઓમાં શોધો..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          leftIcon={<Search size={18} aria-hidden="true" />}
        />
      </div>

      {isLoading ? (
        <LoadingState message="સૂચનાઓ લોડ થઈ રહી છે..." />
      ) : error ? (
        <ErrorState onRetry={() => refetch()} />
      ) : filteredNotices.length === 0 ? (
        <EmptyState
          icon={Newspaper}
          title="હાલ કોઈ સૂચના મળી નથી."
          description={
            searchTerm
              ? "તમે શોધેલી વિગત મુજબ કોઈ સૂચના નથી."
              : "શાળા દ્વારા નવી સૂચના જાહેર થતાં જ અહીં જોવા મળશે."
          }
        />
      ) : (
        <div className="space-y-4">
          {filteredNotices.map((item) => (
            <Card
              key={item.id}
              className="p-5 sm:p-6 transition-all hover:border-blue-200 shadow-xs"
            >
              <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
                <div className="flex items-start gap-3">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-900">
                    <FileText size={20} aria-hidden="true" />
                  </div>
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <Link
                        to={`/notices/${item.id}`}
                        className="text-base sm:text-lg font-bold text-slate-900 hover:text-blue-900 transition-colors"
                      >
                        {item.title}
                      </Link>
                      {item.pinned && (
                        <Badge variant="accent" size="sm" className="gap-1">
                          <Pin size={11} aria-hidden="true" />
                          <span>મહત્વપૂર્ણ</span>
                        </Badge>
                      )}
                    </div>
                    {item.expiresAt && (
                      <div className="mt-1 flex items-center gap-1.5 text-xs text-slate-500">
                        <Calendar size={13} aria-hidden="true" />
                        <span>મુદત: {item.expiresAt.slice(0, 10)}</span>
                      </div>
                    )}
                  </div>
                </div>
                <Link
                  to={`/notices/${item.id}`}
                  className="inline-flex items-center gap-1 text-xs font-semibold text-blue-900 hover:underline shrink-0"
                >
                  <Eye size={14} aria-hidden="true" />
                  <span>વિગત જુઓ</span>
                </Link>
              </div>

              <div className="mt-4 border-t border-slate-100 pt-4 text-sm text-slate-700 whitespace-pre-line leading-relaxed line-clamp-3">
                {item.body}
              </div>

              {item.attachmentFilename && item.attachmentUrl && (
                <div className="mt-4 border-t border-slate-100 pt-3 flex items-center justify-between text-xs text-slate-500">
                  <span>જોડાણ: {item.attachmentFilename}</span>
                  <a
                    href={item.attachmentUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="font-semibold text-blue-900 hover:underline inline-flex items-center gap-1"
                  >
                    <Download size={13} aria-hidden="true" />
                    <span>ડાઉનલોડ કરો</span>
                  </a>
                </div>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
