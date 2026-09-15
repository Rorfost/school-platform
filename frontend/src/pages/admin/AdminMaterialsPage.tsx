import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Trash2, Upload } from "lucide-react";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { MaterialResponse, PageResponse } from "@/api/types";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ConfirmModal } from "@/components/ui/ConfirmModal";
import { Input } from "@/components/ui/Input";
import { LoadingState } from "@/components/common/StatusPanel";

export function AdminMaterialsPage() {
  const queryClient = useQueryClient();
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);

  const { data, isLoading } = useQuery<PageResponse<MaterialResponse>>({
    queryKey: queryKeys.materials({ page: 0, size: 50 }),
    queryFn: () => apiRequest<PageResponse<MaterialResponse>>("/api/v1/public/materials?page=0&size=50"),
  });

  const materials = data?.items ?? [];

  const uploadMutation = useMutation({
    mutationFn: (formData: FormData) =>
      apiRequest<MaterialResponse>("/api/v1/admin/materials", {
        method: "POST",
        body: formData,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.materials() });
      setIsUploadOpen(false);
    },
  });

  const publishMutation = useMutation({
    mutationFn: (id: string) =>
      apiRequest<void>(`/api/v1/admin/materials/${id}/publish`, {
        method: "POST",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.materials() });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) =>
      apiRequest<void>(`/api/v1/admin/materials/${id}`, {
        method: "DELETE",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.materials() });
      setDeleteTargetId(null);
    },
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

  return (
    <div className="space-y-6 max-w-6xl">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Study Materials Management</h1>
          <p className="text-sm text-slate-600 mt-1">
            Upload textbooks, worksheets, and learning resources for Standard 1 to 8 students.
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => setIsUploadOpen(true)}
          className="gap-2 shrink-0"
        >
          <Upload size={16} aria-hidden="true" />
          <span>Upload New Material</span>
        </Button>
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
                        loading={publishMutation.isPending}
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
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <h2 className="text-lg font-bold text-slate-900">Upload Study Material</h2>
            <form onSubmit={handleUploadSubmit} className="space-y-4">
              <Input label="Title" name="title" required placeholder="e.g. Std 3 Maths Worksheet 1" />
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
                <Button variant="outline" size="sm" type="button" onClick={() => setIsUploadOpen(false)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" type="submit" loading={uploadMutation.isPending}>
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
