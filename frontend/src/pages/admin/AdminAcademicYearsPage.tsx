import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Archive, Check, Plus, Star } from "lucide-react";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { AcademicYearRequest, AcademicYearResponse } from "@/api/types";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ConfirmModal } from "@/components/ui/ConfirmModal";
import { Input } from "@/components/ui/Input";
import { LoadingState } from "@/components/common/StatusPanel";

export function AdminAcademicYearsPage() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingYear, setEditingYear] = useState<AcademicYearResponse | null>(null);

  const [confirmModalState, setConfirmModalState] = useState<{
    isOpen: boolean;
    type: "current" | "archive";
    targetId: string;
    targetName: string;
  }>({
    isOpen: false,
    type: "current",
    targetId: "",
    targetName: "",
  });

  const { data: years = [], isLoading } = useQuery<AcademicYearResponse[]>({
    queryKey: ["admin", "academic-years"],
    queryFn: () => apiRequest<AcademicYearResponse[]>("/api/v1/admin/academic-years"),
  });

  const createMutation = useMutation({
    mutationFn: (payload: AcademicYearRequest) =>
      apiRequest<AcademicYearResponse>("/api/v1/admin/academic-years", {
        method: "POST",
        body: payload,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "academic-years"] });
      setIsModalOpen(false);
      setEditingYear(null);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: AcademicYearRequest }) =>
      apiRequest<AcademicYearResponse>(`/api/v1/admin/academic-years/${id}`, {
        method: "PUT",
        body: payload,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "academic-years"] });
      setIsModalOpen(false);
      setEditingYear(null);
    },
  });

  const markCurrentMutation = useMutation({
    mutationFn: (id: string) =>
      apiRequest<AcademicYearResponse>(`/api/v1/admin/academic-years/${id}/current`, {
        method: "POST",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "academic-years"] });
      queryClient.invalidateQueries({ queryKey: queryKeys.currentAcademicYear });
      setConfirmModalState((prev) => ({ ...prev, isOpen: false }));
    },
  });

  const archiveMutation = useMutation({
    mutationFn: (id: string) =>
      apiRequest<AcademicYearResponse>(`/api/v1/admin/academic-years/${id}/archive`, {
        method: "POST",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "academic-years"] });
      setConfirmModalState((prev) => ({ ...prev, isOpen: false }));
    },
  });

  const handleSave = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const payload: AcademicYearRequest = {
      name: String(formData.get("name") || ""),
      startsOn: String(formData.get("startsOn") || ""),
      endsOn: String(formData.get("endsOn") || ""),
      current: formData.get("current") === "on",
    };

    if (editingYear) {
      updateMutation.mutate({ id: editingYear.id, payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  if (isLoading) {
    return <LoadingState message="Loading academic years..." />;
  }

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Academic Years</h1>
          <p className="text-sm text-slate-600 mt-1">
            Configure school sessions, set the active academic year, and archive historical records.
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => {
            setEditingYear(null);
            setIsModalOpen(true);
          }}
          className="gap-2 shrink-0"
        >
          <Plus size={16} aria-hidden="true" />
          <span>New Academic Year</span>
        </Button>
      </div>

      <Card className="overflow-hidden p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-slate-700 font-bold">
                <th className="p-4">Academic Year</th>
                <th className="p-4">Start Date</th>
                <th className="p-4">End Date</th>
                <th className="p-4">Status</th>
                <th className="p-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {years.map((item) => (
                <tr key={item.id} className="hover:bg-slate-50/50">
                  <td className="p-4 font-bold text-slate-900">{item.name}</td>
                  <td className="p-4 text-slate-600">{item.startDate}</td>
                  <td className="p-4 text-slate-600">{item.endDate}</td>
                  <td className="p-4">
                    {item.status === "CURRENT" ? (
                      <Badge variant="primary" size="sm" className="gap-1">
                        <Star size={12} fill="currentColor" aria-hidden="true" />
                        <span>CURRENT</span>
                      </Badge>
                    ) : item.status === "ARCHIVED" ? (
                      <Badge variant="neutral" size="sm">
                        ARCHIVED
                      </Badge>
                    ) : (
                      <Badge variant="secondary" size="sm">
                        ACTIVE
                      </Badge>
                    )}
                  </td>
                  <td className="p-4 text-right space-x-2">
                    {item.status !== "CURRENT" && item.status !== "ARCHIVED" && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          setConfirmModalState({
                            isOpen: true,
                            type: "current",
                            targetId: item.id,
                            targetName: item.name,
                          })
                        }
                        title="Set as Current Academic Year"
                      >
                        <Check size={14} className="text-emerald-700" />
                        <span className="hidden sm:inline">Set Current</span>
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
                            targetName: item.name,
                          })
                        }
                        title="Archive Academic Year"
                      >
                        <Archive size={14} className="text-amber-700" />
                        <span className="hidden sm:inline">Archive</span>
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
              {years.length === 0 && (
                <tr>
                  <td colSpan={5} className="p-8 text-center text-slate-500">
                    No academic years configured. Click 'New Academic Year' to add one.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Create / Edit Form Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <h2 className="text-lg font-bold text-slate-900">
              {editingYear ? "Edit Academic Year" : "Create Academic Year"}
            </h2>
            <form onSubmit={handleSave} className="space-y-4">
              <Input
                label="Session Name"
                name="name"
                required
                defaultValue={editingYear?.name || "2026-27"}
                placeholder="e.g. 2026-27"
              />
              <Input
                label="Start Date"
                name="startsOn"
                type="date"
                required
                defaultValue={editingYear?.startDate || "2026-06-01"}
              />
              <Input
                label="End Date"
                name="endsOn"
                type="date"
                required
                defaultValue={editingYear?.endDate || "2027-05-31"}
              />
              <label className="flex items-center gap-2 text-sm text-slate-700 font-medium">
                <input
                  type="checkbox"
                  name="current"
                  defaultChecked={editingYear?.status === "CURRENT"}
                  className="rounded text-blue-900 focus:ring-blue-900"
                />
                <span>Mark immediately as Current Academic Year</span>
              </label>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="outline" size="sm" type="button" onClick={() => setIsModalOpen(false)}>
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  type="submit"
                  loading={createMutation.isPending || updateMutation.isPending}
                >
                  Save Year
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Confirmation Modal */}
      <ConfirmModal
        isOpen={confirmModalState.isOpen}
        onClose={() => setConfirmModalState((prev) => ({ ...prev, isOpen: false }))}
        onConfirm={() => {
          if (confirmModalState.type === "current") {
            markCurrentMutation.mutate(confirmModalState.targetId);
          } else {
            archiveMutation.mutate(confirmModalState.targetId);
          }
        }}
        title={
          confirmModalState.type === "current"
            ? `Set "${confirmModalState.targetName}" as Current Year?`
            : `Archive Academic Year "${confirmModalState.targetName}"?`
        }
        description={
          confirmModalState.type === "current"
            ? "Setting this year as current will make it the default active session for all public and administrative operations."
            : "Archiving an academic year locks modifications to its past records."
        }
        confirmText={confirmModalState.type === "current" ? "Set Current" : "Archive"}
        variant={confirmModalState.type === "current" ? "primary" : "warning"}
        isLoading={markCurrentMutation.isPending || archiveMutation.isPending}
      />
    </div>
  );
}
