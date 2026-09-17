import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { ApiError, apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type {
  StandardResponse,
  StandardSubjectRequest,
  StandardSubjectResponse,
  SubjectResponse,
} from "@/api/types";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { ErrorState, LoadingState } from "@/components/common/StatusPanel";

export function AdminSubjectMappingsPage() {
  const queryClient = useQueryClient();
  const [selectedStandardId, setSelectedStandardId] = useState<string>("");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const {
    data: standards = [],
    isLoading: isStandardsLoading,
    isError: isStandardsError,
    refetch: refetchStandards,
  } = useQuery<StandardResponse[]>({
    queryKey: queryKeys.adminStandards,
    queryFn: () => apiRequest<StandardResponse[]>("/api/v1/admin/standards"),
  });

  const {
    data: subjects = [],
    isLoading: isSubjectsLoading,
    isError: isSubjectsError,
    refetch: refetchSubjects,
  } = useQuery<SubjectResponse[]>({
    queryKey: queryKeys.adminSubjects,
    queryFn: () => apiRequest<SubjectResponse[]>("/api/v1/admin/subjects"),
  });

  const effectiveStandardId = selectedStandardId || (standards[0]?.id ?? "");

  const {
    data: mappings = [],
    isLoading: isMappingsLoading,
    isError: isMappingsError,
    refetch: refetchMappings,
  } = useQuery<StandardSubjectResponse[]>({
    queryKey: queryKeys.adminStandardSubjects(effectiveStandardId),
    queryFn: () =>
      apiRequest<StandardSubjectResponse[]>(
        `/api/v1/admin/standards/${effectiveStandardId}/subjects`,
      ),
    enabled: Boolean(effectiveStandardId),
  });

  const createMutation = useMutation({
    mutationFn: (payload: StandardSubjectRequest) =>
      apiRequest<StandardSubjectResponse>("/api/v1/admin/standard-subjects", {
        method: "POST",
        body: payload,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.adminStandardSubjects(effectiveStandardId),
      });
      setIsModalOpen(false);
      setErrorMessage(null);
    },
    onError: (err) => {
      setErrorMessage(err instanceof ApiError ? err.message : "Failed to map subject.");
    },
  });

  const handleSave = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const payload: StandardSubjectRequest = {
      standardId: effectiveStandardId,
      subjectId: String(formData.get("subjectId") || ""),
      sortOrder: Number(formData.get("sortOrder") || 1),
    };

    createMutation.mutate(payload);
  };

  const activeStandard = standards.find((s) => s.id === effectiveStandardId);

  if (isStandardsLoading || isSubjectsLoading || (Boolean(effectiveStandardId) && isMappingsLoading)) {
    return <LoadingState message="Loading subject mappings..." />;
  }

  if (isStandardsError || isSubjectsError || isMappingsError) {
    return (
      <ErrorState
        message="Could not load subject mappings."
        onRetry={() => {
          void refetchStandards();
          void refetchSubjects();
          if (effectiveStandardId) void refetchMappings();
        }}
      />
    );
  }

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Standard-Subject Mapping</h1>
          <p className="text-sm text-slate-600 mt-1">
            Map subjects to specific standards and configure display ordering.
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => setIsModalOpen(true)}
          disabled={!effectiveStandardId}
          className="gap-2 shrink-0"
        >
          <Plus size={16} aria-hidden="true" />
          <span>Map Subject to Standard</span>
        </Button>
      </div>

      {/* Select Standard */}
      <Card className="p-4 bg-slate-50 border-slate-200">
        <div className="flex items-center gap-3">
          <label htmlFor="mapping-standard" className="text-sm font-bold text-slate-800 shrink-0">
            Select Standard:
          </label>
          <select
            id="mapping-standard"
            value={effectiveStandardId}
            onChange={(e) => setSelectedStandardId(e.target.value)}
            className="w-full max-w-xs h-10 rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-900 font-semibold focus:border-blue-900 focus:outline-none focus:ring-1 focus:ring-blue-900"
          >
            {standards.map((std) => (
              <option key={std.id} value={std.id}>
                {std.name} ({std.code})
              </option>
            ))}
          </select>
        </div>
      </Card>

      {/* Mappings Table */}
      <Card className="overflow-hidden p-0">
          <div className="p-4 border-b border-slate-100 bg-white flex items-center justify-between">
            <h2 className="font-bold text-slate-900 text-base">
              Mapped Subjects for {activeStandard?.name || "Selected Standard"}
            </h2>
            <span className="text-xs text-slate-500 font-medium">
              {mappings.length} subject(s) mapped
            </span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-slate-700 font-bold">
                  <th className="p-4">Sort Order</th>
                  <th className="p-4">Subject Name</th>
                  <th className="p-4">Subject Code</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {mappings.map((m) => {
                  const subObj = subjects.find((s) => s.id === m.subjectId);
                  return (
                    <tr key={m.id} className="hover:bg-slate-50/50">
                      <td className="p-4 font-mono font-bold text-blue-900">{m.sortOrder}</td>
                      <td className="p-4 font-bold text-slate-900">
                        {subObj?.name || m.subjectId}
                      </td>
                      <td className="p-4 font-mono text-xs text-slate-600">
                        {subObj?.code || "—"}
                      </td>
                    </tr>
                  );
                })}
                {mappings.length === 0 && (
                  <tr>
                    <td colSpan={3} className="p-8 text-center text-slate-500">
                      No subjects mapped to this standard yet. Click 'Map Subject to Standard' to
                      add one.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
      </Card>

      {/* Add Mapping Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <h2 className="text-lg font-bold text-slate-900">
              Map Subject to {activeStandard?.name}
            </h2>
            <form onSubmit={handleSave} className="space-y-4">
              {errorMessage && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700 font-medium">
                  {errorMessage}
                </div>
              )}
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">
                  Select Subject
                </label>
                <select
                  name="subjectId"
                  required
                  className="w-full h-11 rounded-lg border border-slate-300 bg-white px-3.5 text-sm text-slate-900 focus:border-blue-900 focus:outline-none focus:ring-1 focus:ring-blue-900"
                >
                  <option value="">-- Choose Subject --</option>
                  {subjects
                    .filter((sub) => !mappings.some((m) => m.subjectId === sub.id))
                    .map((sub) => (
                      <option key={sub.id} value={sub.id}>
                        {sub.name} ({sub.code})
                      </option>
                    ))}
                </select>
              </div>

              <Input
                label="Sort Order"
                name="sortOrder"
                type="number"
                required
                defaultValue={mappings.length + 1}
              />

              <div className="flex justify-end gap-2 pt-2">
                <Button
                  variant="outline"
                  size="sm"
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                >
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  type="submit"
                  loading={createMutation.isPending}
                >
                  Save Mapping
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
