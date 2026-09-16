import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Edit, Plus } from "lucide-react";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { SubjectRequest, SubjectResponse } from "@/api/types";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { LoadingState } from "@/components/common/StatusPanel";

export function AdminSubjectsPage() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingSubject, setEditingSubject] = useState<SubjectResponse | null>(null);

  const { data: subjects = [], isLoading } = useQuery<SubjectResponse[]>({
    queryKey: queryKeys.adminSubjects,
    queryFn: () => apiRequest<SubjectResponse[]>("/api/v1/admin/subjects"),
  });

  const createMutation = useMutation({
    mutationFn: (payload: SubjectRequest) =>
      apiRequest<SubjectResponse>("/api/v1/admin/subjects", {
        method: "POST",
        body: payload,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.adminSubjects });
      queryClient.invalidateQueries({ queryKey: queryKeys.subjects });
      setIsModalOpen(false);
      setEditingSubject(null);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: SubjectRequest }) =>
      apiRequest<SubjectResponse>(`/api/v1/admin/subjects/${id}`, {
        method: "PUT",
        body: payload,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.adminSubjects });
      queryClient.invalidateQueries({ queryKey: queryKeys.subjects });
      setIsModalOpen(false);
      setEditingSubject(null);
    },
  });

  const handleSave = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const payload: SubjectRequest = {
      code: String(formData.get("code") || ""),
      name: String(formData.get("name") || ""),
      sortOrder: Number(formData.get("sortOrder") || 1),
      archived: formData.get("archived") === "on",
    };

    if (editingSubject) {
      updateMutation.mutate({ id: editingSubject.id, payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  if (isLoading) {
    return <LoadingState message="Loading subjects..." />;
  }

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Subjects Catalog</h1>
          <p className="text-sm text-slate-600 mt-1">
            Configure school curriculum subjects (Gujarati, Maths, EVS, English, Science, etc.).
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => {
            setEditingSubject(null);
            setIsModalOpen(true);
          }}
          className="gap-2 shrink-0"
        >
          <Plus size={16} aria-hidden="true" />
          <span>New Subject</span>
        </Button>
      </div>

      <Card className="overflow-hidden p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-slate-700 font-bold">
                <th className="p-4">Sort Order</th>
                <th className="p-4">Subject Code</th>
                <th className="p-4">Subject Name</th>
                <th className="p-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {subjects.map((item) => (
                <tr key={item.id} className="hover:bg-slate-50/50">
                  <td className="p-4 font-mono font-bold text-blue-900">{item.displayOrder}</td>
                  <td className="p-4 font-mono text-slate-700">{item.code}</td>
                  <td className="p-4 font-bold text-slate-900">{item.name}</td>
                  <td className="p-4 text-right">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        setEditingSubject(item);
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
              {subjects.length === 0 && (
                <tr>
                  <td colSpan={4} className="p-8 text-center text-slate-500">
                    No subjects configured. Click 'New Subject' to add one.
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
              {editingSubject ? "Edit Subject" : "Create Subject"}
            </h2>
            <form onSubmit={handleSave} className="space-y-4">
              <Input
                label="Subject Code"
                name="code"
                required
                defaultValue={editingSubject?.code || ""}
                placeholder="e.g. MATHS"
              />
              <Input
                label="Subject Name (Gujarati / English)"
                name="name"
                required
                defaultValue={editingSubject?.name || ""}
                placeholder="e.g. ગણિત"
              />
              <Input
                label="Sort Order"
                name="sortOrder"
                type="number"
                required
                defaultValue={editingSubject?.displayOrder ?? 1}
              />
              <label className="flex items-center gap-2 text-sm text-slate-700 font-medium">
                <input
                  type="checkbox"
                  name="archived"
                  defaultChecked={false}
                  className="rounded text-blue-900 focus:ring-blue-900"
                />
                <span>Archive Subject</span>
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
                  Save Subject
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
