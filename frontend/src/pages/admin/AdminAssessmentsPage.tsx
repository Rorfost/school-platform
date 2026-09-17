import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Archive, CheckCircle2, FileSpreadsheet, Pencil, Plus } from "lucide-react";
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
import { EmptyState, ErrorState, LoadingState } from "@/components/common/StatusPanel";

type ConfirmAction = "publish" | "archive";

function statusBadge(status: AssessmentResponse["status"]) {
  if (status === "PUBLISHED") {
    return (
      <Badge variant="primary" size="sm" className="gap-1">
        <CheckCircle2 size={12} aria-hidden="true" /> Published
      </Badge>
    );
  }
  if (status === "ARCHIVED")
    return (
      <Badge variant="neutral" size="sm">
        Archived
      </Badge>
    );
  return (
    <Badge variant="secondary" size="sm">
      Draft
    </Badge>
  );
}

export function AdminAssessmentsPage() {
  const queryClient = useQueryClient();
  const [formAssessment, setFormAssessment] = useState<AssessmentResponse | null | undefined>(
    undefined,
  );
  const [selectedStandardId, setSelectedStandardId] = useState("");
  const [yearFilter, setYearFilter] = useState("all");
  const [standardFilter, setStandardFilter] = useState("all");
  const [typeFilter, setTypeFilter] = useState("all");
  const [statusFilter, setStatusFilter] = useState("all");
  const [confirm, setConfirm] = useState<{
    action: ConfirmAction;
    assessment: AssessmentResponse;
  } | null>(null);

  const assessmentsQuery = useQuery<AssessmentResponse[]>({
    queryKey: ["admin", "assessments"],
    queryFn: () => apiRequest<AssessmentResponse[]>("/api/v1/admin/assessments"),
  });
  const yearsQuery = useQuery<AcademicYearResponse[]>({
    queryKey: ["admin", "academic-years"],
    queryFn: () => apiRequest<AcademicYearResponse[]>("/api/v1/admin/academic-years"),
  });
  const standardsQuery = useQuery<StandardResponse[]>({
    queryKey: ["admin", "standards"],
    queryFn: () => apiRequest<StandardResponse[]>("/api/v1/admin/standards"),
  });
  const typesQuery = useQuery<AssessmentTypeResponse[]>({
    queryKey: ["admin", "assessment-types"],
    queryFn: () => apiRequest<AssessmentTypeResponse[]>("/api/v1/admin/assessment-types"),
  });
  const subjectsQuery = useQuery<SubjectResponse[]>({
    queryKey: ["admin", "subjects"],
    queryFn: () => apiRequest<SubjectResponse[]>("/api/v1/admin/subjects"),
  });
  const mappingsQuery = useQuery<StandardSubjectResponse[]>({
    queryKey: ["admin", "standards", selectedStandardId, "subjects"],
    queryFn: () =>
      apiRequest<StandardSubjectResponse[]>(
        `/api/v1/admin/standards/${selectedStandardId}/subjects`,
      ),
    enabled: Boolean(selectedStandardId) && formAssessment === null,
  });

  const years = yearsQuery.data ?? [];
  const standards = standardsQuery.data ?? [];
  const types = typesQuery.data ?? [];
  const subjects = subjectsQuery.data ?? [];
  const editableSubjects =
    formAssessment?.subjects ??
    (mappingsQuery.data ?? []).map((mapping) => ({
      ...mapping,
      maximumMarks: 40,
      passingMarks: 0,
    }));
  const visibleAssessments = useMemo(
    () =>
      (assessmentsQuery.data ?? []).filter(
        (assessment) =>
          (yearFilter === "all" || assessment.academicYearId === yearFilter) &&
          (standardFilter === "all" || assessment.standardId === standardFilter) &&
          (typeFilter === "all" || assessment.assessmentTypeId === typeFilter) &&
          (statusFilter === "all" || assessment.status === statusFilter),
      ),
    [assessmentsQuery.data, statusFilter, standardFilter, typeFilter, yearFilter],
  );
  const invalidateAssessments = () =>
    queryClient.invalidateQueries({ queryKey: ["admin", "assessments"] });

  const saveMutation = useMutation({
    mutationFn: ({ id, payload }: { id?: string; payload: AssessmentRequest }) =>
      apiRequest<AssessmentResponse>(
        id ? `/api/v1/admin/assessments/${id}` : "/api/v1/admin/assessments",
        {
          method: id ? "PUT" : "POST",
          body: payload,
        },
      ),
    onSuccess: () => {
      invalidateAssessments();
      setFormAssessment(undefined);
    },
  });
  const publishMutation = useMutation({
    mutationFn: (id: string) =>
      apiRequest<AssessmentResponse>(`/api/v1/admin/assessments/${id}/publish`, { method: "POST" }),
    onSuccess: () => {
      invalidateAssessments();
      setConfirm(null);
    },
  });
  const archiveMutation = useMutation({
    mutationFn: (id: string) =>
      apiRequest<AssessmentResponse>(`/api/v1/admin/assessments/${id}/archive`, { method: "POST" }),
    onSuccess: () => {
      invalidateAssessments();
      setConfirm(null);
    },
  });

  const openCreate = () => {
    setSelectedStandardId(standards[0]?.id ?? "");
    setFormAssessment(null);
  };
  const openEdit = (assessment: AssessmentResponse) => {
    setSelectedStandardId(assessment.standardId);
    setFormAssessment(assessment);
  };
  const saveAssessment = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const payload: AssessmentRequest = {
      academicYearId: formAssessment?.academicYearId ?? String(values.get("academicYearId") ?? ""),
      standardId: formAssessment?.standardId ?? String(values.get("standardId") ?? ""),
      assessmentTypeId:
        formAssessment?.assessmentTypeId ?? String(values.get("assessmentTypeId") ?? ""),
      title: String(values.get("title") ?? "").trim(),
      description: String(values.get("description") ?? "").trim() || null,
      assessmentDate: String(values.get("assessmentDate") ?? "") || null,
      subjects: editableSubjects.map((subject) => {
        const mappingId = "standardSubjectId" in subject ? subject.standardSubjectId : subject.id;
        return {
          standardSubjectId: mappingId,
          maximumMarks: Number(values.get(`max_${mappingId}`)),
          passingMarks: Number(values.get(`pass_${mappingId}`)),
        };
      }),
    };
    saveMutation.mutate({ id: formAssessment?.id, payload });
  };

  if (assessmentsQuery.isLoading) return <LoadingState message="Loading results and marks..." />;
  if (assessmentsQuery.isError)
    return (
      <ErrorState
        message="Unable to load results and marks."
        onRetry={() => assessmentsQuery.refetch()}
      />
    );

  return (
    <div className="max-w-6xl space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Results &amp; Marks</h1>
          <p className="mt-1 text-sm text-slate-600">
            Set up exams and tests, subjects, and mark limits.
          </p>
        </div>
        <Button
          variant="primary"
          onClick={openCreate}
          disabled={!standards.length}
          className="shrink-0"
        >
          <Plus size={16} aria-hidden="true" /> Add Exam / Test
        </Button>
      </div>

      <Card className="border-amber-200 bg-amber-50/60">
        <div className="flex gap-3">
          <FileSpreadsheet
            className="mt-0.5 shrink-0 text-amber-700"
            size={20}
            aria-hidden="true"
          />
          <div>
            <h2 className="font-semibold text-slate-900">Result upload is not available yet</h2>
            <p className="mt-1 text-sm leading-relaxed text-slate-700">
              The school must first approve a safe roll-number and PIN process. Result upload and
              public result lookup remain unavailable until then. Publishing an exam or test only
              publishes its setup; it does not publish student results.
            </p>
          </div>
        </div>
      </Card>

      <Card>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <select
            aria-label="Filter by academic year"
            value={yearFilter}
            onChange={(event) => setYearFilter(event.target.value)}
            className="h-10 rounded-lg border border-slate-300 bg-white px-3 text-sm"
          >
            <option value="all">All academic years</option>
            {years.map((year) => (
              <option key={year.id} value={year.id}>
                {year.name}
              </option>
            ))}
          </select>
          <select
            aria-label="Filter by standard"
            value={standardFilter}
            onChange={(event) => setStandardFilter(event.target.value)}
            className="h-10 rounded-lg border border-slate-300 bg-white px-3 text-sm"
          >
            <option value="all">All standards</option>
            {standards.map((standard) => (
              <option key={standard.id} value={standard.id}>
                {standard.name}
              </option>
            ))}
          </select>
          <select
            aria-label="Filter by exam or test type"
            value={typeFilter}
            onChange={(event) => setTypeFilter(event.target.value)}
            className="h-10 rounded-lg border border-slate-300 bg-white px-3 text-sm"
          >
            <option value="all">All exam / test types</option>
            {types.map((type) => (
              <option key={type.id} value={type.id}>
                {type.displayName}
              </option>
            ))}
          </select>
          <select
            aria-label="Filter by result status"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
            className="h-10 rounded-lg border border-slate-300 bg-white px-3 text-sm"
          >
            <option value="all">All result statuses</option>
            <option value="DRAFT">Draft</option>
            <option value="PUBLISHED">Published</option>
            <option value="ARCHIVED">Archived</option>
          </select>
        </div>
      </Card>

      {visibleAssessments.length === 0 ? (
        <EmptyState
          title="No exams or tests found"
          description="Add an exam or test, or adjust the filters."
          action={
            <Button size="sm" onClick={openCreate} disabled={!standards.length}>
              Add Exam / Test
            </Button>
          }
        />
      ) : (
        <Card className="overflow-hidden p-0">
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-slate-700">
                  <th className="p-4">Exam / Test</th>
                  <th className="p-4">Academic Year</th>
                  <th className="p-4">Standard</th>
                  <th className="p-4">Result Status</th>
                  <th className="p-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {visibleAssessments.map((assessment) => (
                  <tr key={assessment.id}>
                    <td className="p-4">
                      <p className="font-bold text-slate-900">{assessment.title}</p>
                      <p className="mt-0.5 text-xs text-slate-500">
                        {types.find((type) => type.id === assessment.assessmentTypeId)
                          ?.displayName ?? "Exam / Test type"}
                        {assessment.assessmentDate ? ` · ${assessment.assessmentDate}` : ""}
                      </p>
                    </td>
                    <td className="p-4 text-slate-700">
                      {years.find((year) => year.id === assessment.academicYearId)?.name ??
                        "Unknown year"}
                    </td>
                    <td className="p-4 text-slate-700">
                      {standards.find((standard) => standard.id === assessment.standardId)?.name ??
                        "Unknown standard"}
                    </td>
                    <td className="p-4">{statusBadge(assessment.status)}</td>
                    <td className="p-4">
                      <div className="flex justify-end gap-2">
                        {assessment.status !== "ARCHIVED" && (
                          <Button variant="outline" size="sm" onClick={() => openEdit(assessment)}>
                            <Pencil size={14} aria-hidden="true" /> Edit Exam / Test
                          </Button>
                        )}
                        {assessment.status === "DRAFT" && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setConfirm({ action: "publish", assessment })}
                          >
                            Publish Setup
                          </Button>
                        )}
                        {assessment.status !== "ARCHIVED" && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setConfirm({ action: "archive", assessment })}
                          >
                            <Archive size={14} aria-hidden="true" /> Archive
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {formAssessment !== undefined && (
        <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-slate-900/60 p-4">
          <div className="my-8 w-full max-w-2xl rounded-2xl bg-white p-6 shadow-2xl">
            <h2 className="border-b border-slate-100 pb-3 text-lg font-bold text-slate-900">
              {formAssessment ? "Edit Exam / Test" : "Add Exam / Test"}
            </h2>
            <form onSubmit={saveAssessment} className="mt-4 space-y-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="text-sm font-medium text-slate-700">
                  Academic Year
                  <select
                    name="academicYearId"
                    required
                    disabled={Boolean(formAssessment)}
                    defaultValue={formAssessment?.academicYearId ?? years[0]?.id}
                    className="mt-1 h-11 w-full rounded-lg border border-slate-300 bg-white px-3 disabled:bg-slate-100"
                  >
                    {years.map((year) => (
                      <option key={year.id} value={year.id}>
                        {year.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="text-sm font-medium text-slate-700">
                  Exam / Test Type
                  <select
                    name="assessmentTypeId"
                    required
                    disabled={Boolean(formAssessment)}
                    defaultValue={formAssessment?.assessmentTypeId ?? types[0]?.id}
                    className="mt-1 h-11 w-full rounded-lg border border-slate-300 bg-white px-3 disabled:bg-slate-100"
                  >
                    {types.map((type) => (
                      <option key={type.id} value={type.id}>
                        {type.displayName}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="text-sm font-medium text-slate-700">
                  Standard
                  <select
                    name="standardId"
                    required
                    disabled={Boolean(formAssessment)}
                    value={formAssessment?.standardId ?? selectedStandardId}
                    onChange={(event) => setSelectedStandardId(event.target.value)}
                    className="mt-1 h-11 w-full rounded-lg border border-slate-300 bg-white px-3 disabled:bg-slate-100"
                  >
                    {standards.map((standard) => (
                      <option key={standard.id} value={standard.id}>
                        {standard.name}
                      </option>
                    ))}
                  </select>
                </label>
                <Input
                  label="Exam / Test Date"
                  name="assessmentDate"
                  type="date"
                  defaultValue={formAssessment?.assessmentDate ?? ""}
                />
              </div>
              {formAssessment && (
                <p className="text-xs text-slate-500">
                  Academic Year, Exam / Test Type, and Standard cannot be changed after creation.
                </p>
              )}
              <Input
                label="Exam / Test Name"
                name="title"
                required
                defaultValue={formAssessment?.title ?? ""}
                placeholder="e.g. First unit test"
              />
              <label className="block text-sm font-medium text-slate-700">
                Description
                <textarea
                  name="description"
                  rows={3}
                  defaultValue={formAssessment?.description ?? ""}
                  className="mt-1 w-full rounded-lg border border-slate-300 p-3"
                />
              </label>
              <div className="border-t border-slate-200 pt-4">
                <h3 className="font-semibold text-slate-900">Marks setup</h3>
                {editableSubjects.length === 0 ? (
                  <p className="mt-2 text-sm text-amber-700">
                    Map at least one subject to this Standard before adding an exam or test.
                  </p>
                ) : (
                  <div className="mt-3 space-y-2">
                    {editableSubjects.map((subject) => {
                      const mappingId =
                        "standardSubjectId" in subject ? subject.standardSubjectId : subject.id;
                      const mapping =
                        "subjectId" in subject
                          ? subject
                          : (mappingsQuery.data ?? []).find((item) => item.id === mappingId);
                      const subjectName =
                        subjects.find((item) => item.id === mapping?.subjectId)?.name ??
                        "Mapped subject";
                      return (
                        <div
                          key={mappingId}
                          className="grid grid-cols-[1fr_7rem_7rem] items-end gap-3 rounded-lg bg-slate-50 p-3"
                        >
                          <span className="pb-2 text-sm font-medium text-slate-800">
                            {subjectName}
                          </span>
                          <label className="text-xs text-slate-600">
                            Maximum
                            <input
                              name={`max_${mappingId}`}
                              type="number"
                              min="0.01"
                              step="0.01"
                              required
                              defaultValue={subject.maximumMarks ?? 40}
                              className="mt-1 h-9 w-full rounded border border-slate-300 px-2 text-sm"
                            />
                          </label>
                          <label className="text-xs text-slate-600">
                            Passing
                            <input
                              name={`pass_${mappingId}`}
                              type="number"
                              min="0"
                              step="0.01"
                              required
                              defaultValue={subject.passingMarks ?? 0}
                              className="mt-1 h-9 w-full rounded border border-slate-300 px-2 text-sm"
                            />
                          </label>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
              {saveMutation.isError && (
                <p role="alert" className="text-sm text-red-700">
                  Unable to save the exam or test. Check the selected subjects and mark limits.
                </p>
              )}
              <div className="flex justify-end gap-2 border-t border-slate-100 pt-4">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setFormAssessment(undefined)}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  disabled={editableSubjects.length === 0}
                  loading={saveMutation.isPending}
                >
                  {formAssessment ? "Save changes" : "Add Exam / Test"}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmModal
        isOpen={Boolean(confirm)}
        onClose={() => setConfirm(null)}
        onConfirm={() =>
          confirm &&
          (confirm.action === "publish"
            ? publishMutation.mutate(confirm.assessment.id)
            : archiveMutation.mutate(confirm.assessment.id))
        }
        title={
          confirm?.action === "publish"
            ? `Publish ${confirm.assessment.title} setup?`
            : `Archive ${confirm?.assessment.title ?? "exam or test"}?`
        }
        description={
          confirm?.action === "publish"
            ? "This publishes the exam or test setup only. Student results cannot be uploaded or published yet."
            : "Archiving removes this exam or test from active management. This cannot be undone from the portal."
        }
        confirmText={confirm?.action === "publish" ? "Publish Setup" : "Archive Exam / Test"}
        variant={confirm?.action === "publish" ? "primary" : "warning"}
        isLoading={publishMutation.isPending || archiveMutation.isPending}
      />
    </div>
  );
}
