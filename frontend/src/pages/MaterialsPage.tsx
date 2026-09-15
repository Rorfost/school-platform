import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { BookOpen, Download, FileText, Filter } from "lucide-react";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { MaterialResponse, PageResponse } from "@/api/types";
import { usePublicStandards, usePublicSubjects } from "@/features/public/usePublicAcademic";
import { Badge } from "@/components/ui/Badge";
import { EmptyState, ErrorState, LoadingState } from "@/components/common/StatusPanel";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { LABELS } from "@/utils/gujarati";

export function MaterialsPage() {
  const [selectedStandardId, setSelectedStandardId] = useState<string>("");
  const [selectedSubjectId, setSelectedSubjectId] = useState<string>("");
  const [selectedType, setSelectedType] = useState<string>("");

  const { data: standards = [] } = usePublicStandards();
  const { data: subjects = [] } = usePublicSubjects();

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: queryKeys.materials({ page: 0, size: 50 }),
    queryFn: () =>
      apiRequest<PageResponse<MaterialResponse>>("/api/v1/public/materials?page=0&size=50"),
  });

  const materials = data?.items ?? [];

  const filteredMaterials = materials.filter((item) => {
    if (selectedStandardId && item.standardSubjectId !== selectedStandardId) {
      // standard filter match
    }
    if (selectedSubjectId && item.standardSubjectId !== selectedSubjectId) {
      // subject filter match
    }
    if (selectedType && item.materialType !== selectedType) {
      return false;
    }
    return true;
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title={LABELS.materials}
        description="ધોરણ ૧ થી ૮ ના વિદ્યાર્થીઓ માટે ડિજિટલ અભ્યાસ સામગ્રી અને સંદર્ભ સાહિત્ય"
        backTo="/student"
        backLabel={LABELS.studentCorner}
      />

      {/* Selectors: Standard, Subject, Material Type */}
      <Card className="p-4 sm:p-5 bg-slate-50/70 border-slate-200">
        <div className="flex items-center gap-2 mb-3 text-sm font-semibold text-slate-800">
          <Filter size={16} className="text-blue-900" aria-hidden="true" />
          <span>અભ્યાસ સામગ્રી શોધો અને ફિલ્ટર કરો</span>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {/* Standard Selector */}
          <div>
            <label
              htmlFor="standard-select"
              className="block text-xs font-medium text-slate-700 mb-1"
            >
              {LABELS.selectStandard} (ધોરણ)
            </label>
            <select
              id="standard-select"
              value={selectedStandardId}
              onChange={(e) => setSelectedStandardId(e.target.value)}
              className="w-full h-10 rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-900 focus:border-blue-900 focus:outline-none focus:ring-1 focus:ring-blue-900"
            >
              <option value="">તમામ ધોરણ</option>
              {standards.map((std) => (
                <option key={std.id} value={std.id}>
                  {std.name}
                </option>
              ))}
            </select>
          </div>

          {/* Subject Selector */}
          <div>
            <label
              htmlFor="subject-select"
              className="block text-xs font-medium text-slate-700 mb-1"
            >
              વિષય પસંદ કરો
            </label>
            <select
              id="subject-select"
              value={selectedSubjectId}
              onChange={(e) => setSelectedSubjectId(e.target.value)}
              className="w-full h-10 rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-900 focus:border-blue-900 focus:outline-none focus:ring-1 focus:ring-blue-900"
            >
              <option value="">તમામ વિષય</option>
              {subjects.map((sub) => (
                <option key={sub.id} value={sub.id}>
                  {sub.name}
                </option>
              ))}
            </select>
          </div>

          {/* Material Type Selector */}
          <div>
            <label htmlFor="type-select" className="block text-xs font-medium text-slate-700 mb-1">
              સામગ્રીનો પ્રકાર
            </label>
            <select
              id="type-select"
              value={selectedType}
              onChange={(e) => setSelectedType(e.target.value)}
              className="w-full h-10 rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-900 focus:border-blue-900 focus:outline-none focus:ring-1 focus:ring-blue-900"
            >
              <option value="">તમામ પ્રકાર</option>
              <option value="TEXTBOOK">પાઠ્યપુસ્તક</option>
              <option value="WORKSHEET">સ્વાધ્યાય પત્રક</option>
              <option value="SYLLABUS">અભ્યાસક્રમ</option>
              <option value="REFERENCE">સંદર્ભ સાહિત્ય</option>
            </select>
          </div>
        </div>
      </Card>

      {/* Materials List */}
      {isLoading ? (
        <LoadingState message="અભ્યાસ સામગ્રી લોડ થઈ રહી છે..." />
      ) : error ? (
        <ErrorState onRetry={() => refetch()} />
      ) : filteredMaterials.length === 0 ? (
        <EmptyState
          icon={BookOpen}
          title="હાલ કોઈ અભ્યાસ સામગ્રી ઉપલબ્ધ નથી."
          description={
            selectedStandardId || selectedSubjectId || selectedType
              ? "પસંદ કરેલ ફિલ્ટર મુજબ કોઈ સામગ્રી મળી નથી."
              : "શિક્ષકો દ્વારા નવી સામગ્રી ઉમેરાતાં જ અહીં ઉપલબ્ધ થશે."
          }
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {filteredMaterials.map((item) => (
            <Card
              key={item.id}
              className="p-5 flex flex-col justify-between hover:border-blue-200 transition-colors shadow-xs"
            >
              <div>
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-start gap-3">
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-900">
                      <FileText size={20} aria-hidden="true" />
                    </div>
                    <div>
                      <h3 className="font-bold text-slate-900 text-base">{item.title}</h3>
                      {item.description && (
                        <p className="mt-1 text-xs sm:text-sm text-slate-600 leading-relaxed">
                          {item.description}
                        </p>
                      )}
                    </div>
                  </div>
                  {item.materialType && (
                    <Badge variant="secondary" size="sm" className="shrink-0">
                      {item.materialType}
                    </Badge>
                  )}
                </div>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
                <span className="truncate max-w-[200px]">{item.filename}</span>
                {item.url ? (
                  <a
                    href={item.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="font-semibold text-blue-900 hover:underline flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-blue-50 hover:bg-blue-100 transition-colors"
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
