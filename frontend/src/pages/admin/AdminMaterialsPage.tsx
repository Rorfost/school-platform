import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Trash2, Upload } from "lucide-react";
import { ApiError, apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type {
  AcademicYearResponse,
  MaterialResponse,
  PageResponse,
  StandardResponse,
  StandardSubjectResponse,
  SubjectResponse,
} from "@/api/types";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ConfirmModal } from "@/components/ui/ConfirmModal";
import { Input } from "@/components/ui/Input";
import { ErrorState, LoadingState } from "@/components/common/StatusPanel";

export function AdminMaterialsPage() {
  const queryClient = useQueryClient();
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<"ALL" | "PUBLISHED" | "DRAFT">("ALL");
  const [selectedStandardId, setSelectedStandardId] = useState<string>("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const { data: years = [] } = useQuery<AcademicYearResponse[]>({
    queryKey: queryKeys.adminAcademicYears,
    queryFn: () => apiRequest<AcademicYearResponse[]>("/api/v1/admin/academic-years"),
  });

  const { data: standards = [] } = useQuery<StandardResponse[]>({
    queryKey: queryKeys.adminStandards,
    queryFn: () => apiRequest<StandardResponse[]>("/api/v1/admin/standards"),
  });

  const { data: subjects = [] } = useQuery<SubjectResponse[]>({
    queryKey: queryKeys.adminSubjects,
    queryFn: () => apiRequest<SubjectResponse[]>("/api/v1/admin/subjects"),
  });

  const { data: mappings = [] } = useQuery<StandardSubjectResponse[]>({
    queryKey: queryKeys.adminStandardSubjects(selectedStandardId),
    queryFn: () =>
      apiRequest<StandardSubjectResponse[]>(
        `/api/v1/admin/standards/${selectedStandardId}/subjects`,
      ),
    enabled: Boolean(selectedStandardId),
  });

  const { data, isLoading, isError, refetch } = useQuery<PageResponse<MaterialResponse>>({
    queryKey: queryKeys.adminMaterials({ page: 0, size: 50 }),
    queryFn: () =>
      apiRequest<PageResponse<MaterialResponse>>("/api/v1/admin/materials?page=0&size=50"),
  });

  const allMaterials = data?.items ?? [];
  const currentAcademicYearId = years.find((year) => year.status === "CURRENT")?.id ?? "";
  const materials = allMaterials.filter((item) => {
    if (statusFilter === "PUBLISHED") return item.status === "PUBLISHED";
    if (statusFilter === "DRAFT") return item.status === "DRAFT";
    return true;
  });

  const uploadMutation = useMutation({
    mutationFn: (formData: FormData) =>
      apiRequest<MaterialResponse>("/api/v1/admin/materials", {
        method: "POST",
        body: formData,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.adminMaterials() });
      setIsUploadOpen(false);
      setErrorMessage(null);
    },
    onError: (err) => {
      setErrorMessage(err instanceof ApiError ? err.message : "Failed to upload study material.");
    },
  });

  const publishMutation = useMutation({
    mutationFn: (id: string) =>
      apiRequest<void>(`/api/v1/admin/materials/${id}/publish`, {
        method: "POST",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.adminMaterials() });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) =>
      apiRequest<void>(`/api/v1/admin/materials/${id}`, {
        method: "DELETE",
      }),
    onSuccess: (_, id) => {
      queryClient.setQueryData<PageResponse<MaterialResponse>>(
        queryKeys.adminMaterials({ page: 0, size: 50 }),
        (current) =>
          current
            ? { ...current, items: current.items.filter((item) => item.id !== id), totalItems: current.totalItems - 1 }
            : current,
      );
      queryClient.invalidateQueries({ queryKey: queryKeys.adminMaterials() });
      setDeleteTargetId(null);
    },
    onError: (error) =>
      setErrorMessage(error instanceof ApiError ? error.message : "Could not delete study material."),
  });

  const handleUploadSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const formData = new FormData(form);
    uploadMutation.mutate(formData);
  };

  if (isLoading) {
    return <LoadingState message="Loading study materials..." />;
  }

  if (isError) {
    return <ErrorState message="Could not load study materials." onRetry={() => refetch()} />;
  }

  return (
    <div className="space-y-6 max-w-6xl">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Study Materials Management</h1>
          <p className="text-sm text-slate-600 mt-1">
            Upload textbooks, worksheets, and learning resources for Standard 1 to 8 students.
          </p>
        </div>
        <Button variant="primary" onClick={() => setIsUploadOpen(true)} className="gap-2 shrink-0">
          <Upload size={16} aria-hidden="true" />
          <span>Upload New Material</span>
        </Button>
      </div>

      {/* Status Filter Tabs */}
      <div className="flex border-b border-slate-200 gap-4 text-sm font-semibold">
        {(["ALL", "PUBLISHED", "DRAFT"] as const).map((st) => (
          <button
            key={st}
            onClick={() => setStatusFilter(st)}
            className={`pb-2 transition-colors border-b-2 ${
              statusFilter === st
                ? "border-blue-900 text-blue-900 font-bold"
                : "border-transparent text-slate-500 hover:text-slate-700"
            }`}
          >
            {st} (
            {st === "ALL"
              ? allMaterials.length
              : allMaterials.filter((m) => m.status === st).length}
            )
          </button>
        ))}
      </div>

      <Card className="overflow-hidden p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-slate-700 font-bold">
                <th className="p-4">Material Title</th>
                <th className="p-4">Type</th>
                <th className="p-4">Filename</th>
                <th className="p-4">Status</th>
                <th className="p-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {materials.map((item) => (
                <tr key={item.id} className="hover:bg-slate-50/50">
                  <td className="p-4">
                    <div className="font-bold text-slate-900">{item.title}</div>
                    {item.description && (
                      <div className="text-xs text-slate-500 mt-0.5 truncate max-w-xs">
                        {item.description}
                      </div>
                    )}
                    {(item.standardName || item.subjectName) && (
                      <div className="mt-2 flex flex-wrap gap-1.5 text-xs font-medium text-slate-600">
                        {item.standardName && <Badge variant="neutral" size="sm">{item.standardName}</Badge>}
                        {item.subjectName && <Badge variant="neutral" size="sm">{item.subjectName}</Badge>}
                      </div>
                    )}
                  </td>
                  <td className="p-4">
                    <Badge variant="secondary" size="sm">
                      {item.materialType}
                    </Badge>
                  </td>
                  <td className="p-4 text-slate-600 text-xs font-mono">{item.filename}</td>
                  <td className="p-4">
                    {item.status === "PUBLISHED" ? (
                      <Badge variant="primary" size="sm" className="gap-1">
                        <CheckCircle2 size={12} />
                        <span>PUBLISHED</span>
                      </Badge>
                    ) : (
                      <Badge variant="neutral" size="sm">
                        {item.status}
                      </Badge>
                    )}
                  </td>
                  <td className="p-4 text-right space-x-2">
                    {item.status !== "PUBLISHED" && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => publishMutation.mutate(item.id)}
                        loading={publishMutation.isPending && publishMutation.variables === item.id}
                      >
                        Publish
                      </Button>
                    )}
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setDeleteTargetId(item.id)}
                      className="text-red-600 hover:bg-red-50"
                    >
                      <Trash2 size={14} />
                    </Button>
                  </td>
                </tr>
              ))}
              {materials.length === 0 && (
                <tr>
                  <td colSpan={5} className="p-8 text-center text-slate-500">
                    No study materials uploaded. Click 'Upload New Material' to add one.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Upload Modal */}
      {isUploadOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <h2 className="text-lg font-bold text-slate-900">Upload Study Material</h2>
            <form onSubmit={handleUploadSubmit} className="space-y-4">
              {errorMessage && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700 font-medium">
                  {errorMessage}
                </div>
              )}
              <Input
                label="Title"
                name="title"
                required
                placeholder="e.g. Std 3 Maths Worksheet 1"
              />

              <div>
                <label
                  htmlFor="material-academic-year"
                  className="block text-sm font-medium text-slate-700 mb-1"
                >
                  Academic Session
                </label>
                <select
                  id="material-academic-year"
                  name="academicYearId"
                  defaultValue={currentAcademicYearId}
                  className="w-full h-11 rounded-lg border border-slate-300 bg-white px-3.5 text-sm text-slate-900"
                >
                  <option value="">-- Optional Academic Year --</option>
                  {years.map((y) => (
                    <option key={y.id} value={y.id}>
                      {y.name} {y.status === "CURRENT" ? "(Current)" : ""}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Standard and subject
                </label>
                <div className="grid grid-cols-2 gap-2 mb-2">
                  <select
                    value={selectedStandardId}
                    onChange={(e) => setSelectedStandardId(e.target.value)}
                    className="min-h-11 w-full rounded-lg border border-slate-300 bg-white px-2.5 text-xs font-medium text-slate-900"
                  >
                    <option value="">-- Select Standard --</option>
                    {standards.map((std) => (
                      <option key={std.id} value={std.id}>
                        {std.displayName}
                      </option>
                    ))}
                  </select>

                  <select
                    name="standardSubjectId"
                    disabled={!selectedStandardId}
                    className="min-h-11 w-full rounded-lg border border-slate-300 bg-white px-2.5 text-xs font-medium text-slate-900 disabled:bg-slate-100 disabled:text-slate-400"
                  >
                    <option value="">-- Select Subject --</option>
                    {mappings.map((m) => {
                      const sub = subjects.find((s) => s.id === m.subjectId);
                      return (
                        <option key={m.id} value={m.id}>
                          {sub?.name || "Subject"}
                        </option>
                      );
                    })}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Material Type
                </label>
                <select
                  name="materialType"
                  required
                  className="w-full h-11 rounded-lg border border-slate-300 bg-white px-3.5 text-sm text-slate-900"
                >
                  <option value="TEXTBOOK">Textbook (પાઠ્યપુસ્તક)</option>
                  <option value="WORKSHEET">Worksheet (સ્વાધ્યાય પત્રક)</option>
                  <option value="SYLLABUS">Syllabus (અભ્યાસક્રમ)</option>
                  <option value="REFERENCE">Reference (સંદર્ભ સાહિત્ય)</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Description</label>
                <textarea
                  name="description"
                  rows={2}
                  className="w-full rounded-lg border border-slate-300 p-2.5 text-sm text-slate-900"
                  placeholder="Optional material description..."
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Select File (PDF, Image, Doc) <span className="text-red-500">*</span>
                </label>
                <input
                  type="file"
                  name="file"
                  required
                  className="w-full text-xs text-slate-600 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:bg-blue-50 file:text-blue-900 file:font-semibold hover:file:bg-blue-100"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button
                  variant="outline"
                  size="sm"
                  type="button"
                  onClick={() => {
                    setIsUploadOpen(false);
                    setErrorMessage(null);
                  }}
                >
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  type="submit"
                  loading={uploadMutation.isPending}
                >
                  Upload Material
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation */}
      <ConfirmModal
        isOpen={Boolean(deleteTargetId)}
        onClose={() => setDeleteTargetId(null)}
        onConfirm={() => deleteTargetId && deleteMutation.mutate(deleteTargetId)}
        title="Delete Study Material?"
        description="Are you sure you want to delete this study material? This action cannot be undone."
        confirmText="Delete"
        variant="danger"
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}
