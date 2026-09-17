import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Archive, Pencil, Plus, Settings2, Trash2 } from "lucide-react";
import { ApiError, apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { AcademicSetupResponse, StandardResponse, SubjectResponse } from "@/api/types";
import { ContentSkeleton, EmptyState, ErrorState } from "@/components/common/StatusPanel";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ConfirmModal } from "@/components/ui/ConfirmModal";
import { Input } from "@/components/ui/Input";

type Confirmation =
  | { action: "delete-standard" | "archive-standard"; item: StandardResponse }
  | { action: "delete-subject" | "archive-subject"; item: SubjectResponse };

function academicError(error: unknown, fallback: string) {
  if (!(error instanceof ApiError)) return fallback;
  return (
    {
      standard_duplicate: "This Standard already exists.",
      standard_in_use:
        "This Standard is already used in school records. You can deactivate it instead.",
      subject_duplicate: "This Subject already exists.",
      subject_in_use:
        "This Subject is already used in school records. You can deactivate it instead.",
      standard_subject_in_use:
        "This Subject is used by existing materials or assessments and cannot be removed from this Standard.",
    }[error.code ?? ""] ?? fallback
  );
}

export function AdminAcademicSetupPage() {
  const queryClient = useQueryClient();
  const [editingStandard, setEditingStandard] = useState<
    AcademicSetupResponse["standards"][number] | null
  >(null);
  const [selectedSubjectIds, setSelectedSubjectIds] = useState<string[]>([]);
  const [standardTarget, setStandardTarget] = useState<StandardResponse | null>(null);
  const [subjectTarget, setSubjectTarget] = useState<SubjectResponse | null>(null);
  const [standardDialog, setStandardDialog] = useState(false);
  const [subjectDialog, setSubjectDialog] = useState(false);
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const setupQuery = useQuery<AcademicSetupResponse>({
    queryKey: queryKeys.adminAcademicSetup,
    queryFn: () => apiRequest("/api/v1/admin/academic-setup"),
    placeholderData: (previous) => previous,
  });
  const updateSetup = (updater: (setup: AcademicSetupResponse) => AcademicSetupResponse) =>
    queryClient.setQueryData<AcademicSetupResponse>(queryKeys.adminAcademicSetup, (current) =>
      current ? updater(current) : current,
    );
  const refreshCounts = () => {
    void queryClient.invalidateQueries({ queryKey: queryKeys.adminDashboardSummary });
    void queryClient.invalidateQueries({ queryKey: queryKeys.standards });
    void queryClient.invalidateQueries({ queryKey: queryKeys.subjects });
  };
  const closeStandardDialog = () => {
    setStandardDialog(false);
    setStandardTarget(null);
  };
  const closeSubjectDialog = () => {
    setSubjectDialog(false);
    setSubjectTarget(null);
  };
  const saveMappings = useMutation({
    mutationFn: ({ standardId, subjectIds }: { standardId: string; subjectIds: string[] }) =>
      apiRequest<SubjectResponse[]>(`/api/v1/admin/standards/${standardId}/subjects`, {
        method: "PUT",
        body: { subjectIds },
      }),
    onSuccess: (subjects, { standardId }) => {
      updateSetup((setup) => ({
        ...setup,
        standards: setup.standards.map((entry) =>
          entry.standard.id === standardId ? { ...entry, subjects } : entry,
        ),
      }));
      setEditingStandard(null);
      setErrorMessage(null);
    },
    onError: (error) => setErrorMessage(academicError(error, "Could not save Subjects.")),
  });
  const saveStandard = useMutation({
    mutationFn: ({
      standard,
      displayName,
    }: {
      standard: StandardResponse | null;
      displayName: string;
    }) =>
      standard
        ? apiRequest<StandardResponse>(`/api/v1/admin/standards/${standard.id}`, {
            method: "PUT",
            body: { ...standard, displayName },
          })
        : apiRequest<StandardResponse>("/api/v1/admin/standards/catalog", {
            method: "POST",
            body: { displayName },
          }),
    onSuccess: (standard, { standard: existing }) => {
      updateSetup((setup) => ({
        ...setup,
        standards: existing
          ? setup.standards.map((entry) =>
              entry.standard.id === standard.id ? { ...entry, standard } : entry,
            )
          : [...setup.standards, { standard, subjects: [] }],
      }));
      refreshCounts();
      closeStandardDialog();
      setErrorMessage(null);
    },
    onError: (error) => setErrorMessage(academicError(error, "Could not save the Standard.")),
  });
  const saveSubject = useMutation({
    mutationFn: ({ subject, name }: { subject: SubjectResponse | null; name: string }) =>
      subject
        ? apiRequest<SubjectResponse>(`/api/v1/admin/subjects/${subject.id}`, {
            method: "PUT",
            body: { ...subject, name },
          })
        : apiRequest<SubjectResponse>("/api/v1/admin/subjects/catalog", {
            method: "POST",
            body: { name },
          }),
    onSuccess: (subject, { subject: existing }) => {
      updateSetup((setup) => ({
        ...setup,
        subjects: existing
          ? setup.subjects.map((item) => (item.id === subject.id ? subject : item))
          : [...setup.subjects, subject],
        standards: setup.standards.map((entry) => ({
          ...entry,
          subjects: entry.subjects.map((item) => (item.id === subject.id ? subject : item)),
        })),
      }));
      refreshCounts();
      closeSubjectDialog();
      setErrorMessage(null);
    },
    onError: (error) => setErrorMessage(academicError(error, "Could not save the Subject.")),
  });
  const deleteStandard = useMutation({
    mutationFn: (id: string) =>
      apiRequest<void>(`/api/v1/admin/standards/${id}`, { method: "DELETE" }),
    onSuccess: (_, id) => {
      updateSetup((setup) => ({
        ...setup,
        standards: setup.standards.filter((entry) => entry.standard.id !== id),
      }));
      refreshCounts();
      setConfirmation(null);
    },
    onError: (error) => {
      setConfirmation(null);
      setErrorMessage(academicError(error, "Could not delete the Standard."));
    },
  });
  const archiveStandard = useMutation({
    mutationFn: (id: string) =>
      apiRequest<StandardResponse>(`/api/v1/admin/standards/${id}/archive`, { method: "POST" }),
    onSuccess: (_, id) => {
      updateSetup((setup) => ({
        ...setup,
        standards: setup.standards.filter((entry) => entry.standard.id !== id),
      }));
      refreshCounts();
      setConfirmation(null);
    },
    onError: (error) => {
      setConfirmation(null);
      setErrorMessage(academicError(error, "Could not deactivate the Standard."));
    },
  });
  const removeSubjectFromSetup = (id: string) =>
    updateSetup((setup) => ({
      ...setup,
      subjects: setup.subjects.filter((subject) => subject.id !== id),
      standards: setup.standards.map((entry) => ({
        ...entry,
        subjects: entry.subjects.filter((subject) => subject.id !== id),
      })),
    }));
  const deleteSubject = useMutation({
    mutationFn: (id: string) =>
      apiRequest<void>(`/api/v1/admin/subjects/${id}`, { method: "DELETE" }),
    onSuccess: (_, id) => {
      removeSubjectFromSetup(id);
      refreshCounts();
      setConfirmation(null);
    },
    onError: (error) => {
      setConfirmation(null);
      setErrorMessage(academicError(error, "Could not delete the Subject."));
    },
  });
  const archiveSubject = useMutation({
    mutationFn: (id: string) =>
      apiRequest<SubjectResponse>(`/api/v1/admin/subjects/${id}/archive`, { method: "POST" }),
    onSuccess: (_, id) => {
      removeSubjectFromSetup(id);
      refreshCounts();
      setConfirmation(null);
    },
    onError: (error) => {
      setConfirmation(null);
      setErrorMessage(academicError(error, "Could not deactivate the Subject."));
    },
  });

  if (setupQuery.isPending)
    return (
      <div className="max-w-6xl space-y-6" aria-busy="true">
        <div className="h-8 w-64 rounded bg-slate-200" />
        <ContentSkeleton rows={4} />
      </div>
    );
  if (setupQuery.isError)
    return (
      <ErrorState
        message="Unable to load academic setup."
        onRetry={() => void setupQuery.refetch()}
      />
    );
  const setup = setupQuery.data;
  if (!setup) return null;
  const pendingConfirmation =
    deleteStandard.isPending ||
    archiveStandard.isPending ||
    deleteSubject.isPending ||
    archiveSubject.isPending;
  const isDelete = confirmation?.action.includes("delete") ?? false;
  const confirmTitle = isDelete
    ? `Delete ${confirmation?.action.includes("subject") ? "Subject" : "Standard"}?`
    : `Deactivate ${confirmation?.action.includes("subject") ? "Subject" : "Standard"}?`;
  const confirmDescription = isDelete
    ? "This only succeeds when it is not used in historical school records. It cannot be undone."
    : "This keeps historical records intact and removes the item from new academic setup.";

  return (
    <div className="max-w-6xl space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Standards &amp; Subjects</h1>
          <p className="mt-1 text-sm text-slate-600">
            Add Standards, then choose the Subjects taught in each one.
          </p>
          {setupQuery.isFetching && (
            <p className="mt-2 text-xs text-slate-500" role="status">
              Refreshing academic setup...
            </p>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <Button
            size="sm"
            onClick={() => {
              setStandardTarget(null);
              setStandardDialog(true);
            }}
          >
            <Plus size={16} aria-hidden="true" /> Add Standard
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              setSubjectTarget(null);
              setSubjectDialog(true);
            }}
          >
            <Plus size={16} aria-hidden="true" /> Add Subject
          </Button>
        </div>
      </div>
      {errorMessage && (
        <p
          role="alert"
          className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800"
        >
          {errorMessage}
        </p>
      )}
      {setup.standards.length === 0 ? (
        <EmptyState
          title="No Standards yet."
          description="Add your first Standard to begin assigning Subjects."
          action={
            <Button size="sm" onClick={() => setStandardDialog(true)}>
              <Plus size={16} aria-hidden="true" /> Add Standard
            </Button>
          }
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {setup.standards.map((entry) => (
            <Card key={entry.standard.id} className="flex flex-col gap-4">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h2 className="text-lg font-bold text-slate-900">{entry.standard.displayName}</h2>
                  <p className="mt-1 text-sm text-slate-600">
                    {entry.subjects.length
                      ? `${entry.subjects.length} subject${entry.subjects.length === 1 ? "" : "s"} selected`
                      : "No Subjects assigned yet."}
                  </p>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    setEditingStandard(entry);
                    setSelectedSubjectIds(entry.subjects.map((subject) => subject.id));
                    setErrorMessage(null);
                  }}
                >
                  <Settings2 size={15} aria-hidden="true" /> Manage Subjects
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
                  <span className="text-sm text-slate-500">No Subjects assigned yet.</span>
                )}
              </div>
              <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-3">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setStandardTarget(entry.standard);
                    setStandardDialog(true);
                  }}
                >
                  <Pencil size={14} aria-hidden="true" /> Edit
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-red-700 hover:bg-red-50"
                  onClick={() =>
                    setConfirmation({ action: "delete-standard", item: entry.standard })
                  }
                >
                  <Trash2 size={14} aria-hidden="true" /> Delete
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-amber-800 hover:bg-amber-50"
                  onClick={() =>
                    setConfirmation({ action: "archive-standard", item: entry.standard })
                  }
                >
                  <Archive size={14} aria-hidden="true" /> Deactivate
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}
      <Card className="space-y-4">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Subject Catalogue</h2>
            <p className="text-sm text-slate-600">
              Delete removes a Subject globally only when no school records use it.
            </p>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              setSubjectTarget(null);
              setSubjectDialog(true);
            }}
          >
            <Plus size={16} aria-hidden="true" /> Add Subject
          </Button>
        </div>
        {setup.subjects.length === 0 ? (
          <p className="text-sm text-slate-500">No Subjects yet. Add your first Subject.</p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {setup.subjects.map((subject) => (
              <li
                key={subject.id}
                className="flex flex-col gap-3 py-3 sm:flex-row sm:items-center sm:justify-between"
              >
                <span className="font-medium text-slate-900">{subject.name}</span>
                <div className="flex flex-wrap gap-2">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setSubjectTarget(subject);
                      setSubjectDialog(true);
                    }}
                  >
                    <Pencil size={14} aria-hidden="true" /> Edit
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-red-700 hover:bg-red-50"
                    onClick={() => setConfirmation({ action: "delete-subject", item: subject })}
                  >
                    <Trash2 size={14} aria-hidden="true" /> Delete Subject
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-amber-800 hover:bg-amber-50"
                    onClick={() => setConfirmation({ action: "archive-subject", item: subject })}
                  >
                    <Archive size={14} aria-hidden="true" /> Deactivate
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>
      {editingStandard && (
        <Dialog
          title={`Manage Subjects: ${editingStandard.standard.displayName}`}
          onClose={() => !saveMappings.isPending && setEditingStandard(null)}
        >
          <p className="text-sm text-slate-600">
            Unchecking a Subject removes it from this Standard only; it does not delete the Subject.
          </p>
          <div className="mt-4 space-y-2">
            {setup.subjects.map((subject) => {
              const checked = selectedSubjectIds.includes(subject.id);
              return (
                <label
                  key={subject.id}
                  className="flex cursor-pointer items-center gap-3 rounded-lg border border-slate-200 p-3"
                >
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={() =>
                      setSelectedSubjectIds((ids) =>
                        checked ? ids.filter((id) => id !== subject.id) : [...ids, subject.id],
                      )
                    }
                    className="size-4"
                  />
                  <span>{subject.name}</span>
                </label>
              );
            })}
          </div>
          <div className="mt-5 flex justify-end gap-2">
            <Button
              variant="outline"
              onClick={() => setEditingStandard(null)}
              disabled={saveMappings.isPending}
            >
              Cancel
            </Button>
            <Button
              loading={saveMappings.isPending}
              onClick={() =>
                saveMappings.mutate({
                  standardId: editingStandard.standard.id,
                  subjectIds: selectedSubjectIds,
                })
              }
            >
              {saveMappings.isPending ? "Saving..." : "Save Subjects"}
            </Button>
          </div>
        </Dialog>
      )}
      {standardDialog && (
        <NameDialog
          title={standardTarget ? "Edit Standard" : "Add Standard"}
          label="Standard Name"
          defaultValue={standardTarget?.displayName}
          submitText={standardTarget ? "Save Standard" : "Add Standard"}
          pending={saveStandard.isPending}
          onClose={() => !saveStandard.isPending && closeStandardDialog()}
          onSubmit={(displayName) => saveStandard.mutate({ standard: standardTarget, displayName })}
        />
      )}
      {subjectDialog && (
        <NameDialog
          title={subjectTarget ? "Edit Subject" : "Add Subject"}
          label="Subject Name"
          defaultValue={subjectTarget?.name}
          submitText={subjectTarget ? "Save Subject" : "Add Subject"}
          pending={saveSubject.isPending}
          onClose={() => !saveSubject.isPending && closeSubjectDialog()}
          onSubmit={(name) => saveSubject.mutate({ subject: subjectTarget, name })}
        />
      )}
      <ConfirmModal
        isOpen={!!confirmation}
        onClose={() => setConfirmation(null)}
        title={confirmTitle}
        description={confirmDescription}
        confirmText={
          pendingConfirmation
            ? isDelete
              ? "Deleting..."
              : "Deactivating..."
            : isDelete
              ? `Delete ${confirmation?.action.includes("subject") ? "Subject" : "Standard"}`
              : `Deactivate ${confirmation?.action.includes("subject") ? "Subject" : "Standard"}`
        }
        variant={isDelete ? "danger" : "warning"}
        isLoading={pendingConfirmation}
        onConfirm={() => {
          if (!confirmation) return;
          if (confirmation.action === "delete-standard")
            deleteStandard.mutate(confirmation.item.id);
          else if (confirmation.action === "archive-standard")
            archiveStandard.mutate(confirmation.item.id);
          else if (confirmation.action === "delete-subject")
            deleteSubject.mutate(confirmation.item.id);
          else archiveSubject.mutate(confirmation.item.id);
        }}
      />
    </div>
  );
}

function Dialog({
  title,
  children,
  onClose,
}: {
  title: string;
  children: React.ReactNode;
  onClose: () => void;
}) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4"
      role="dialog"
      aria-modal="true"
      aria-label={title}
      onClick={onClose}
    >
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 className="text-lg font-bold text-slate-900">{title}</h2>
        {children}
      </div>
    </div>
  );
}

function NameDialog({
  title,
  label,
  defaultValue,
  submitText,
  pending,
  onClose,
  onSubmit,
}: {
  title: string;
  label: string;
  defaultValue?: string;
  submitText: string;
  pending: boolean;
  onClose: () => void;
  onSubmit: (value: string) => void;
}) {
  return (
    <Dialog title={title} onClose={onClose}>
      <form
        className="mt-4 space-y-4"
        onSubmit={(event) => {
          event.preventDefault();
          const value = String(new FormData(event.currentTarget).get("name") ?? "").trim();
          if (value) onSubmit(value);
        }}
      >
        <Input
          autoFocus
          label={label}
          name="name"
          defaultValue={defaultValue}
          required
          placeholder={label === "Standard Name" ? "e.g. Standard 5" : "e.g. Mathematics"}
        />
        <div className="flex justify-end gap-2">
          <Button variant="outline" type="button" disabled={pending} onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={pending}>
            {pending ? "Saving..." : submitText}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}
