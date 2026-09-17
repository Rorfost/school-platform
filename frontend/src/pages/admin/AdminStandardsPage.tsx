import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Edit } from "lucide-react";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { StandardRequest, StandardResponse } from "@/api/types";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { ErrorState, LoadingState } from "@/components/common/StatusPanel";

export function AdminStandardsPage() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingStandard, setEditingStandard] = useState<StandardResponse | null>(null);

  const { data: standards = [], isLoading, isError, refetch } = useQuery<StandardResponse[]>({
    queryKey: queryKeys.adminStandards,
    queryFn: () => apiRequest<StandardResponse[]>("/api/v1/admin/standards"),
  });

  const createMutation = useMutation({
    mutationFn: (payload: StandardRequest) =>
      apiRequest<StandardResponse>("/api/v1/admin/standards", {
        method: "POST",
        body: payload,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.adminStandards });
      queryClient.invalidateQueries({ queryKey: queryKeys.standards });
      setIsModalOpen(false);
      setEditingStandard(null);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: StandardRequest }) =>
      apiRequest<StandardResponse>(`/api/v1/admin/standards/${id}`, {
        method: "PUT",
        body: payload,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.adminStandards });
      queryClient.invalidateQueries({ queryKey: queryKeys.standards });
      setIsModalOpen(false);
      setEditingStandard(null);
    },
  });

  const handleSave = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const payload: StandardRequest = {
      code: String(formData.get("code") || ""),
      displayName: String(formData.get("displayName") || ""),
      sortOrder: Number(formData.get("sortOrder") || 1),
      archived: formData.get("archived") === "on",
    };

    if (editingStandard) {
      updateMutation.mutate({ id: editingStandard.id, payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  if (isLoading) {
    return <LoadingState message="Loading standards..." />;
  }

  if (isError) {
    return <ErrorState message="Could not load standards." onRetry={() => refetch()} />;
  }

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Standards & Classes</h1>
          <p className="text-sm text-slate-600 mt-1">
            Configure Standard 1 to 8 classes, display names, and ordering.
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => {
            setEditingStandard(null);
            setIsModalOpen(true);
          }}
          className="gap-2 shrink-0"
        >
          <Plus size={16} aria-hidden="true" />
          <span>New Standard</span>
        </Button>
      </div>

      <Card className="overflow-hidden p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-slate-700 font-bold">
                <th className="p-4">Sort Order</th>
                <th className="p-4">Code</th>
                <th className="p-4">Display Name (Gujarati)</th>
                <th className="p-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {standards.map((item) => (
                <tr key={item.id} className="hover:bg-slate-50/50">
                  <td className="p-4 font-mono font-bold text-blue-900">{item.displayOrder}</td>
                  <td className="p-4 font-mono text-slate-700">{item.code}</td>
                  <td className="p-4 font-bold text-slate-900">{item.name}</td>
                  <td className="p-4 text-right">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        setEditingStandard(item);
                        setIsModalOpen(true);
                      }}
                      className="gap-1.5"
                    >
                      <Edit size={14} />
                      <span>Edit</span>
                    </Button>
                  </td>
                </tr>
              ))}
              {standards.length === 0 && (
                <tr>
                  <td colSpan={4} className="p-8 text-center text-slate-500">
                    No standards configured. Click 'New Standard' to add one.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Modal Form */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <h2 className="text-lg font-bold text-slate-900">
              {editingStandard ? "Edit Standard" : "Create Standard"}
            </h2>
            <form onSubmit={handleSave} className="space-y-4">
              <Input
                label="Standard Code"
                name="code"
                required
                defaultValue={editingStandard?.code || ""}
                placeholder="e.g. STD_3"
              />
              <Input
                label="Display Name (Gujarati)"
                name="displayName"
                required
                defaultValue={editingStandard?.name || ""}
                placeholder="e.g. ધોરણ ૩"
              />
              <Input
                label="Sort Order"
                name="sortOrder"
                type="number"
                required
                defaultValue={editingStandard?.displayOrder ?? 1}
              />
              <label className="flex items-center gap-2 text-sm text-slate-700 font-medium">
                <input
                  type="checkbox"
                  name="archived"
                  defaultChecked={false}
                  className="rounded text-blue-900 focus:ring-blue-900"
                />
                <span>Archive Standard</span>
              </label>

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
                  loading={createMutation.isPending || updateMutation.isPending}
                >
                  Save Standard
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
