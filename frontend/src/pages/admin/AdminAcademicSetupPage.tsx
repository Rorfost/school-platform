import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Archive, BookOpen, Pencil, Plus, Settings2 } from "lucide-react";
import { ApiError, apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { AcademicSetupResponse, StandardResponse, SubjectResponse } from "@/api/types";
import { ErrorState, LoadingState } from "@/components/common/StatusPanel";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";

export function AdminAcademicSetupPage() {
  const queryClient = useQueryClient();
  const [editingStandard, setEditingStandard] = useState<
    AcademicSetupResponse["standards"][number] | null
  >(null);
  const [selectedSubjectIds, setSelectedSubjectIds] = useState<string[]>([]);
  const [isSubjectDialogOpen, setIsSubjectDialogOpen] = useState(false);
  const [standardFormTarget, setStandardFormTarget] = useState<StandardResponse | null>(null);
  const [isStandardDialogOpen, setIsStandardDialogOpen] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const setupQuery = useQuery<AcademicSetupResponse>({
    queryKey: queryKeys.adminAcademicSetup,
    queryFn: () => apiRequest<AcademicSetupResponse>("/api/v1/admin/academic-setup"),
  });

  const invalidateSetup = () => {
    void queryClient.invalidateQueries({ queryKey: queryKeys.adminAcademicSetup });
    void queryClient.invalidateQueries({ queryKey: queryKeys.adminStandards });
    void queryClient.invalidateQueries({ queryKey: queryKeys.adminSubjects });
  };
  const saveSubjectsMutation = useMutation({
    mutationFn: ({ standardId, subjectIds }: { standardId: string; subjectIds: string[] }) =>
      apiRequest<SubjectResponse[]>(`/api/v1/admin/standards/${standardId}/subjects`, {
        method: "PUT",
        body: { subjectIds },
      }),
    onSuccess: () => {
      invalidateSetup();
      setEditingStandard(null);
      setErrorMessage(null);
    },
    onError: (error) => {
      setErrorMessage(error instanceof ApiError ? error.message : "Unable to save subjects.");
    },
  });
  const createSubjectMutation = useMutation({
    mutationFn: (name: string) =>
      apiRequest<SubjectResponse>("/api/v1/admin/subjects/catalog", {
        method: "POST",
        body: { name },
      }),
    onSuccess: () => {
      invalidateSetup();
      setIsSubjectDialogOpen(false);
      setErrorMessage(null);
    },
    onError: (error) => {
      setErrorMessage(error instanceof ApiError ? error.message : "Unable to add the subject.");
    },
  });
  const saveStandardMutation = useMutation({
    mutationFn: ({
      standard,
      displayName,
    }: {
      standard: StandardResponse | null;
      displayName: string;
    }) => {
      const body = {
        // Codes are an internal database key; principals manage the readable standard name only.
        code: standard?.code ?? `STANDARD_${Date.now()}`,
        displayName,
        sortOrder: standard?.sortOrder ?? (setupQuery.data?.standards.length ?? 0) + 1,
        archived: false,
      };
      return apiRequest<StandardResponse>(
        standard ? `/api/v1/admin/standards/${standard.id}` : "/api/v1/admin/standards",
        { method: standard ? "PUT" : "POST", body },
      );
    },
    onSuccess: () => {
      invalidateSetup();
      setIsStandardDialogOpen(false);
      setStandardFormTarget(null);
      setErrorMessage(null);
    },
    onError: (error) =>
      setErrorMessage(error instanceof ApiError ? error.message : "Unable to save the standard."),
  });
  const archiveStandardMutation = useMutation({
    mutationFn: (standard: StandardResponse) =>
      apiRequest<StandardResponse>(`/api/v1/admin/standards/${standard.id}`, {
        method: "PUT",
        body: { ...standard, archived: true },
      }),
    onSuccess: () => {
      invalidateSetup();
      setErrorMessage(null);
    },
    onError: (error) =>
      setErrorMessage(
        error instanceof ApiError ? error.message : "Unable to deactivate the standard.",
      ),
  });

  const openSubjectManager = (standard: AcademicSetupResponse["standards"][number]) => {
    setEditingStandard(standard);
    setSelectedSubjectIds(standard.subjects.map((subject) => subject.id));
    setErrorMessage(null);
  };

  if (setupQuery.isLoading) return <LoadingState message="Loading academic setup..." />;
  if (setupQuery.isError)
    return (
      <ErrorState message="Unable to load academic setup." onRetry={() => setupQuery.refetch()} />
    );

  const setup = setupQuery.data;
  if (!setup) return null;

  return (
    <div className="max-w-6xl space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Academic Setup</h1>
          <p className="mt-1 text-sm text-slate-600">
            Add the standards your school uses, then choose the subjects taught in each one.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              setStandardFormTarget(null);
              setIsStandardDialogOpen(true);
            }}
          >
            <Plus size={16} aria-hidden="true" /> Add standard
          </Button>
          <Button variant="primary" size="sm" onClick={() => setIsSubjectDialogOpen(true)}>
            <Plus size={16} aria-hidden="true" /> Add subject
          </Button>
        </div>
      </div>

      {setup.subjects.length === 0 && (
        <Card className="border-amber-200 bg-amber-50">
          <p className="text-sm text-amber-900">
            Add your first subject, then assign it to the standards where it is taught.
          </p>
        </Card>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        {setup.standards.map((entry) => (
          <Card key={entry.standard.id} className="flex flex-col gap-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h2 className="text-lg font-bold text-slate-900">{entry.standard.displayName}</h2>
                <p className="mt-1 text-sm text-slate-600">
                  {entry.subjects.length
                    ? `${entry.subjects.length} subject${entry.subjects.length === 1 ? "" : "s"} selected`
                    : "No subjects selected yet"}
                </p>
              </div>
              <Button variant="outline" size="sm" onClick={() => openSubjectManager(entry)}>
                <Settings2 size={15} aria-hidden="true" /> Manage subjects
              </Button>
            </div>
            <div className="flex flex-wrap gap-2">
              {entry.subjects.length ? (
                entry.subjects.map((subject) => (
                  <Badge key={subject.id} variant="secondary" size="sm">
                    {subject.name}
                  </Badge>
                ))
              ) : (
                <span className="text-sm text-slate-500">Select subjects for this standard.</span>
              )}
            </div>
            <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-3">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setStandardFormTarget(entry.standard);
                  setIsStandardDialogOpen(true);
                }}
              >
                <Pencil size={14} aria-hidden="true" /> Edit
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="text-amber-800 hover:bg-amber-50"
                onClick={() => archiveStandardMutation.mutate(entry.standard)}
                loading={
                  archiveStandardMutation.isPending &&
                  archiveStandardMutation.variables?.id === entry.standard.id
                }
              >
                <Archive size={14} aria-hidden="true" /> Deactivate
              </Button>
            </div>
          </Card>
        ))}
      </div>

      {editingStandard && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
          <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl">
            <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
              <BookOpen size={20} className="text-blue-900" aria-hidden="true" />
              <div>
                <h2 className="text-lg font-bold text-slate-900">
                  {editingStandard.standard.displayName}
                </h2>
                <p className="text-sm text-slate-600">
                  Select the subjects taught in this standard.
                </p>
              </div>
            </div>
            {errorMessage && (
              <p role="alert" className="mt-4 text-sm text-red-700">
                {errorMessage}
              </p>
            )}
            <div className="mt-4 space-y-2">
              {setup.subjects.map((subject) => {
                const isSelected = selectedSubjectIds.includes(subject.id);
                return (
                  <label
                    key={subject.id}
                    className="flex cursor-pointer items-center gap-3 rounded-lg border border-slate-200 p-3 hover:bg-slate-50"
                  >
                    <input
                      type="checkbox"
                      checked={isSelected}
                      onChange={() =>
                        setSelectedSubjectIds((selected) =>
                          isSelected
                            ? selected.filter((id) => id !== subject.id)
                            : [...selected, subject.id],
                        )
                      }
                      className="size-4 rounded border-slate-300 text-blue-900"
                    />
                    <span className="text-sm font-medium text-slate-800">{subject.name}</span>
                  </label>
                );
              })}
            </div>
            <div className="mt-5 flex justify-end gap-2 border-t border-slate-100 pt-4">
              <Button variant="outline" onClick={() => setEditingStandard(null)}>
                Cancel
              </Button>
              <Button
                onClick={() =>
                  saveSubjectsMutation.mutate({
                    standardId: editingStandard.standard.id,
                    subjectIds: selectedSubjectIds,
                  })
                }
                loading={saveSubjectsMutation.isPending}
              >
                Save subjects
              </Button>
            </div>
          </div>
        </div>
      )}

      {isSubjectDialogOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
            <h2 className="text-lg font-bold text-slate-900">Add subject</h2>
            <form
              className="mt-4 space-y-4"
              onSubmit={(event) => {
                event.preventDefault();
                const name = String(new FormData(event.currentTarget).get("name") ?? "").trim();
                if (name) createSubjectMutation.mutate(name);
              }}
            >
              {errorMessage && (
                <p role="alert" className="text-sm text-red-700">
                  {errorMessage}
                </p>
              )}
              <Input label="Subject name" name="name" required placeholder="e.g. Mathematics" />
              <div className="flex justify-end gap-2 border-t border-slate-100 pt-4">
                <Button
                  variant="outline"
                  type="button"
                  onClick={() => setIsSubjectDialogOpen(false)}
                >
                  Cancel
                </Button>
                <Button type="submit" loading={createSubjectMutation.isPending}>
                  Add subject
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
      {isStandardDialogOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
            <h2 className="text-lg font-bold text-slate-900">
              {standardFormTarget ? "Edit standard" : "Add standard"}
            </h2>
            <form
              className="mt-4 space-y-4"
              onSubmit={(event) => {
                event.preventDefault();
                const displayName = String(
                  new FormData(event.currentTarget).get("displayName") ?? "",
                ).trim();
                if (displayName)
                  saveStandardMutation.mutate({ standard: standardFormTarget, displayName });
              }}
            >
              <Input
                label="Standard name"
                name="displayName"
                defaultValue={standardFormTarget?.displayName}
                required
                placeholder="e.g. Standard 3"
              />
              <div className="flex justify-end gap-2 border-t border-slate-100 pt-4">
                <Button
                  variant="outline"
                  type="button"
                  onClick={() => setIsStandardDialogOpen(false)}
                >
                  Cancel
                </Button>
                <Button type="submit" loading={saveStandardMutation.isPending}>
                  Save standard
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
