import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Archive, CheckCircle2, FileSpreadsheet, Plus, Star } from "lucide-react";
import { apiRequest } from "@/api/client";
import type {
  AcademicYearResponse,
  AssessmentRequest,
  AssessmentResponse,
  StandardResponse,
  StandardSubjectResponse,
  SubjectResponse,
} from "@/api/types";
import type { AssessmentTypeResponse } from "@/features/public/usePublicAcademic";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ConfirmModal } from "@/components/ui/ConfirmModal";
import { Input } from "@/components/ui/Input";
import { LoadingState } from "@/components/common/StatusPanel";

export function AdminAssessmentsPage() {
  const queryClient = useQueryClient();
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isExcelInfoOpen, setIsExcelInfoOpen] = useState(false);

  const [confirmModalState, setConfirmModalState] = useState<{
    isOpen: boolean;
    type: "publish" | "archive";
    targetId: string;
    targetTitle: string;
  }>({
    isOpen: false,
    type: "publish",
    targetId: "",
    targetTitle: "",
  });

  const { data: assessments = [], isLoading } = useQuery<AssessmentResponse[]>({
    queryKey: ["admin", "assessments"],
    queryFn: () => apiRequest<AssessmentResponse[]>("/api/v1/admin/assessments"),
  });

  const { data: years = [] } = useQuery<AcademicYearResponse[]>({
    queryKey: ["admin", "academic-years"],
    queryFn: () => apiRequest<AcademicYearResponse[]>("/api/v1/admin/academic-years"),
  });

  const { data: standards = [] } = useQuery<StandardResponse[]>({
    queryKey: ["admin", "standards"],
    queryFn: () => apiRequest<StandardResponse[]>("/api/v1/admin/standards"),
  });

  const { data: assessmentTypes = [] } = useQuery<AssessmentTypeResponse[]>({
    queryKey: ["admin", "assessment-types"],
    queryFn: () => apiRequest<AssessmentTypeResponse[]>("/api/v1/admin/assessment-types"),
  });

  const { data: subjectsCatalog = [] } = useQuery<SubjectResponse[]>({
    queryKey: ["admin", "subjects"],
    queryFn: () => apiRequest<SubjectResponse[]>("/api/v1/admin/subjects"),
  });

  // Selected Standard in Create Assessment Form
  const [selectedStandardId, setSelectedStandardId] = useState<string>("");

  const { data: mappedSubjects = [] } = useQuery<StandardSubjectResponse[]>({
    queryKey: ["admin", "standards", selectedStandardId, "subjects"],
    queryFn: () =>
      apiRequest<StandardSubjectResponse[]>(
        `/api/v1/admin/standards/${selectedStandardId}/subjects`,
      ),
    enabled: Boolean(selectedStandardId),
  });

  const createMutation = useMutation({
    mutationFn: (payload: AssessmentRequest) =>
      apiRequest<AssessmentResponse>("/api/v1/admin/assessments", {
        method: "POST",
        body: payload,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "assessments"] });
      setIsCreateModalOpen(false);
    },
  });

  const publishMutation = useMutation({
    mutationFn: (id: string) =>
      apiRequest<AssessmentResponse>(`/api/v1/admin/assessments/${id}/publish`, {
        method: "POST",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "assessments"] });
      setConfirmModalState((prev) => ({ ...prev, isOpen: false }));
    },
  });

  const archiveMutation = useMutation({
    mutationFn: (id: string) =>
      apiRequest<AssessmentResponse>(`/api/v1/admin/assessments/${id}/archive`, {
        method: "POST",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "assessments"] });
      setConfirmModalState((prev) => ({ ...prev, isOpen: false }));
    },
  });

  const handleCreateSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const academicYearId = String(formData.get("academicYearId") || "");
    const standardId = String(formData.get("standardId") || "");
    const assessmentTypeId = String(formData.get("assessmentTypeId") || "");
    const title = String(formData.get("title") || "");
    const description = String(formData.get("description") || "");
    const assessmentDate = String(formData.get("assessmentDate") || "");

    const subjectRequests = mappedSubjects.map((mapping) => ({
      standardSubjectId: mapping.id,
      maximumMarks: Number(formData.get(`max_${mapping.id}`) || 40),
      passingMarks: Number(formData.get(`pass_${mapping.id}`) || 14),
    }));

    const payload: AssessmentRequest = {
      academicYearId,
      standardId,
      assessmentTypeId,
      title,
      description: description || null,
      assessmentDate: assessmentDate || null,
      subjects: subjectRequests,
    };

    createMutation.mutate(payload);
  };

  if (isLoading) {
    return <LoadingState message="Loading assessments..." />;
  }

  return (
    <div className="space-y-6 max-w-6xl">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Assessment & Exam Management</h1>
          <p className="text-sm text-slate-600 mt-1">
            Configure generic assessments, Ekam Kasoti exams, subject mark limits, and publish
            results.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Button
            variant="outline"
            onClick={() => setIsExcelInfoOpen(true)}
            className="gap-2 shrink-0 text-slate-700"
          >
            <FileSpreadsheet size={16} className="text-emerald-700" />
            <span>Excel Result Import Info</span>
          </Button>
          <Button
            variant="primary"
            onClick={() => {
              if (standards.length > 0) setSelectedStandardId(standards[0].id);
              setIsCreateModalOpen(true);
            }}
            className="gap-2 shrink-0"
          >
            <Plus size={16} aria-hidden="true" />
            <span>New Assessment</span>
          </Button>
        </div>
      </div>

      <Card className="overflow-hidden p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-slate-700 font-bold">
                <th className="p-4">Assessment Title</th>
                <th className="p-4">Standard</th>
                <th className="p-4">Date</th>
                <th className="p-4">Status</th>
                <th className="p-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {assessments.map((item) => {
                const stdObj = standards.find((s) => s.id === item.standardId);
                return (
                  <tr key={item.id} className="hover:bg-slate-50/50">
                    <td className="p-4">
                      <div className="font-bold text-slate-900">{item.title}</div>
                      {item.description && (
                        <div className="text-xs text-slate-500 mt-0.5 truncate max-w-xs">
                          {item.description}
                        </div>
                      )}
                    </td>
                    <td className="p-4 font-medium text-slate-800">
                      {stdObj?.name || item.standardId}
                    </td>
                    <td className="p-4 text-slate-600">{item.assessmentDate || "N/A"}</td>
                    <td className="p-4">
                      {item.status === "PUBLISHED" ? (
                        <Badge variant="primary" size="sm" className="gap-1">
                          <CheckCircle2 size={12} aria-hidden="true" />
                          <span>PUBLISHED</span>
                        </Badge>
                      ) : item.status === "ARCHIVED" ? (
                        <Badge variant="neutral" size="sm">
                          ARCHIVED
                        </Badge>
                      ) : (
                        <Badge variant="secondary" size="sm">
                          DRAFT
                        </Badge>
                      )}
                    </td>
                    <td className="p-4 text-right space-x-2">
                      {item.status === "DRAFT" && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() =>
                            setConfirmModalState({
                              isOpen: true,
                              type: "publish",
                              targetId: item.id,
                              targetTitle: item.title,
                            })
                          }
                          title="Publish Assessment Results"
                        >
                          <Star size={14} className="text-emerald-700" />
                          <span className="hidden sm:inline">Publish</span>
                        </Button>
                      )}
                      {item.status !== "ARCHIVED" && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() =>
                            setConfirmModalState({
                              isOpen: true,
                              type: "archive",
                              targetId: item.id,
                              targetTitle: item.title,
                            })
                          }
                          title="Archive Assessment"
                        >
                          <Archive size={14} className="text-amber-700" />
                          <span className="hidden sm:inline">Archive</span>
                        </Button>
                      )}
                    </td>
                  </tr>
                );
              })}
              {assessments.length === 0 && (
                <tr>
                  <td colSpan={5} className="p-8 text-center text-slate-500">
                    No assessments configured. Click 'New Assessment' to create one.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Create Assessment Modal */}
      {isCreateModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl p-6 max-w-2xl w-full shadow-2xl space-y-4 my-8">
            <h2 className="text-lg font-bold text-slate-900 border-b border-slate-100 pb-3">
              Create New Assessment
            </h2>
            <form onSubmit={handleCreateSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Academic Session <span className="text-red-500">*</span>
                  </label>
                  <select
                    name="academicYearId"
                    required
                    className="w-full h-11 rounded-lg border border-slate-300 bg-white px-3.5 text-sm text-slate-900"
                  >
                    {years.map((y) => (
                      <option key={y.id} value={y.id}>
                        {y.name} ({y.status})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Assessment Type <span className="text-red-500">*</span>
                  </label>
                  <select
                    name="assessmentTypeId"
                    required
                    className="w-full h-11 rounded-lg border border-slate-300 bg-white px-3.5 text-sm text-slate-900"
                  >
                    {assessmentTypes.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Standard / Class <span className="text-red-500">*</span>
                  </label>
                  <select
                    name="standardId"
                    required
                    value={selectedStandardId}
                    onChange={(e) => setSelectedStandardId(e.target.value)}
                    className="w-full h-11 rounded-lg border border-slate-300 bg-white px-3.5 text-sm text-slate-900"
                  >
                    {standards.map((std) => (
                      <option key={std.id} value={std.id}>
                        {std.name}
                      </option>
                    ))}
                  </select>
                </div>

                <Input
                  label="Assessment Date"
                  name="assessmentDate"
                  type="date"
                  defaultValue="2026-09-15"
                />
              </div>

              <Input
                label="Assessment Title"
                name="title"
                required
                defaultValue="ત્રિમાસિક એકમ કસોટી ૨૦૨૬-૨૭"
                placeholder="e.g. ત્રિમાસિક એકમ કસોટી ૨૦૨૬-૨૭"
              />

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Description / Instructions
                </label>
                <textarea
                  name="description"
                  rows={2}
                  className="w-full rounded-lg border border-slate-300 p-2.5 text-sm text-slate-900"
                  placeholder="Additional exam details..."
                />
              </div>

              {/* Subject Marks Setup */}
              <div className="border-t border-slate-200 pt-4">
                <h3 className="font-bold text-slate-900 text-sm mb-3">
                  Subject Marks Configuration ({mappedSubjects.length} subjects mapped)
                </h3>
                {mappedSubjects.length === 0 ? (
                  <p className="text-xs text-amber-700 bg-amber-50 p-3 rounded-lg border border-amber-200">
                    No subjects mapped to this standard yet. Map subjects in 'Subject Mappings'
                    first.
                  </p>
                ) : (
                  <div className="space-y-3 max-h-48 overflow-y-auto pr-1">
                    {mappedSubjects.map((mapping) => {
                      const subObj = subjectsCatalog.find((s) => s.id === mapping.subjectId);
                      return (
                        <div
                          key={mapping.id}
                          className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-3 bg-slate-50 rounded-lg border border-slate-200 text-sm"
                        >
                          <span className="font-bold text-slate-900 sm:w-1/3">
                            {subObj?.name || mapping.subjectId}
                          </span>
                          <div className="flex items-center gap-3">
                            <div>
                              <label className="block text-[11px] font-semibold text-slate-500">
                                Max Marks
                              </label>
                              <input
                                type="number"
                                name={`max_${mapping.id}`}
                                required
                                defaultValue={40}
                                className="w-20 h-8 rounded border border-slate-300 px-2 text-xs text-center"
                              />
                            </div>
                            <div>
                              <label className="block text-[11px] font-semibold text-slate-500">
                                Passing Marks
                              </label>
                              <input
                                type="number"
                                name={`pass_${mapping.id}`}
                                required
                                defaultValue={14}
                                className="w-20 h-8 rounded border border-slate-300 px-2 text-xs text-center"
                              />
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <Button
                  variant="outline"
                  size="sm"
                  type="button"
                  onClick={() => setIsCreateModalOpen(false)}
                >
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  type="submit"
                  disabled={mappedSubjects.length === 0}
                  loading={createMutation.isPending}
                >
                  Create Assessment
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Excel Info Modal */}
      {isExcelInfoOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-lg w-full shadow-2xl space-y-4">
            <div className="flex items-center gap-3 text-emerald-900 font-bold text-lg border-b border-slate-100 pb-3">
              <FileSpreadsheet size={24} className="text-emerald-700" />
              <span>Excel Result Import Contract Status</span>
            </div>
            <div className="text-sm text-slate-700 space-y-3 leading-relaxed">
              <p>
                <strong>Tri-masik Ekam Kasoti Standard 3 Workbook Contract:</strong>
              </p>
              <ul className="list-disc pl-5 space-y-1 text-xs text-slate-600">
                <li>
                  Observed structure: 4 Subject Columns (Gujarati 40, Maths 40, EVS 40, English 40 =
                  Total 160).
                </li>
                <li>
                  Automatic computation: Totals are recomputed server-side from subject marks.
                </li>
                <li>
                  <strong>Pending Contract Status:</strong> Excel parsing and student roll-number
                  mapping are blocked per <code>docs/data/RESULT_IMPORT.md</code> until school owner
                  supplies roll-number distribution contract.
                </li>
              </ul>
              <p className="text-xs bg-slate-50 p-3 rounded-lg border border-slate-200 text-slate-600">
                Assessments created here are automatically available for public lookup test
                simulation once published.
              </p>
            </div>
            <div className="flex justify-end pt-2">
              <Button variant="primary" size="sm" onClick={() => setIsExcelInfoOpen(false)}>
                Got it
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Modal */}
      <ConfirmModal
        isOpen={confirmModalState.isOpen}
        onClose={() => setConfirmModalState((prev) => ({ ...prev, isOpen: false }))}
        onConfirm={() => {
          if (confirmModalState.type === "publish") {
            publishMutation.mutate(confirmModalState.targetId);
          } else {
            archiveMutation.mutate(confirmModalState.targetId);
          }
        }}
        title={
          confirmModalState.type === "publish"
            ? `Publish Assessment "${confirmModalState.targetTitle}"?`
            : `Archive Assessment "${confirmModalState.targetTitle}"?`
        }
        description={
          confirmModalState.type === "publish"
            ? "Publishing this assessment makes student marks available to individual public lookup."
            : "Archiving removes this assessment from active listings."
        }
        confirmText={confirmModalState.type === "publish" ? "Publish" : "Archive"}
        variant={confirmModalState.type === "publish" ? "primary" : "warning"}
        isLoading={publishMutation.isPending || archiveMutation.isPending}
      />
    </div>
  );
}
