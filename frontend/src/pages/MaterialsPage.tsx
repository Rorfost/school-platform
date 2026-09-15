import { useQuery } from "@tanstack/react-query";
import { BookOpen, Download, FileText } from "lucide-react";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { MaterialResponse, PageResponse } from "@/api/types";
import { EmptyState, ErrorState, LoadingState } from "@/components/common/StatusPanel";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { LABELS } from "@/utils/gujarati";

export function MaterialsPage() {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: queryKeys.materials({ page: 0, size: 20 }),
    queryFn: () => apiRequest<PageResponse<MaterialResponse>>("/api/v1/public/materials?page=0&size=20"),
  });

  const materials = data?.items ?? [];

  return (
    <div className="space-y-6">
      <PageHeader
        title={LABELS.materials}
        description="ધોરણ ૧ થી ૮ ના વિદ્યાર્થીઓ માટે ડિજિટલ અભ્યાસ સામગ્રી અને સંદર્ભ સાહિત્ય"
        backTo="/student"
        backLabel={LABELS.studentCorner}
      />

      {isLoading ? (
        <LoadingState message="અભ્યાસ સામગ્રી લોડ થઈ રહી છે..." />
      ) : error ? (
        <ErrorState onRetry={() => refetch()} />
      ) : materials.length === 0 ? (
        <EmptyState
          icon={BookOpen}
          title="હાલ કોઈ અભ્યાસ સામગ્રી ઉપલબ્ધ નથી."
          description="શિક્ષકો દ્વારા નવી સામગ્રી ઉમેરાતાં જ અહીં ઉપલબ્ધ થશે."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {materials.map((item) => (
            <Card key={item.id} className="p-5 flex flex-col justify-between">
              <div>
                <div className="flex items-start gap-3">
                  <FileText className="text-blue-900 shrink-0 mt-0.5" size={20} aria-hidden="true" />
                  <div>
                    <h3 className="font-semibold text-slate-900">{item.title}</h3>
                    {item.description && (
                      <p className="mt-1 text-sm text-slate-600">{item.description}</p>
                    )}
                  </div>
                </div>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
                <span>{item.originalFilename}</span>
                <span className="font-medium text-blue-900 flex items-center gap-1">
                  <Download size={13} aria-hidden="true" />
                  <span>ડાઉનલોડ</span>
                </span>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
