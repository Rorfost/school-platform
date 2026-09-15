import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Paperclip, Pin, Plus, Trash2 } from "lucide-react";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { NoticeRequest, NoticeResponse, PageResponse } from "@/api/types";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ConfirmModal } from "@/components/ui/ConfirmModal";
import { Input } from "@/components/ui/Input";
import { LoadingState } from "@/components/common/StatusPanel";

export function AdminNoticesPage() {
  const queryClient = useQueryClient();
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [attachTargetId, setAttachTargetId] = useState<string | null>(null);
  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);

  const { data, isLoading } = useQuery<PageResponse<NoticeResponse>>({
    queryKey: queryKeys.notices({ page: 0, size: 50 }),
    queryFn: () => apiRequest<PageResponse<NoticeResponse>>("/api/v1/public/notices?page=0&size=50"),
  });

  const notices = data?.items ?? [];

  const createMutation = useMutation({
    mutationFn: (payload: NoticeRequest) =>
      apiRequest<NoticeResponse>("/api/v1/admin/notices", {
        method: "POST",
        body: payload,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.notices() });
      setIsCreateOpen(false);
    },
  });

  const attachMutation = useMutation({
    mutationFn: ({ id, formData }: { id: string; formData: FormData }) =>
      apiRequest<NoticeResponse>(`/api/v1/admin/notices/${id}/attachment`, {
        method: "POST",
        body: formData,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.notices() });
      setAttachTargetId(null);
    },
  });

  const publishMutation = useMutation({
    mutationFn: (id: string) =>
      apiRequest<void>(`/api/v1/admin/notices/${id}/publish`, {
        method: "POST",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.notices() });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) =>
      apiRequest<void>(`/api/v1/admin/notices/${id}`, {
        method: "DELETE",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.notices() });
      setDeleteTargetId(null);
    },
  });

  const handleCreateSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const title = String(formData.get("title") || "");
    const body = String(formData.get("body") || "");
    const pinned = formData.get("pinned") === "on";
    const expiresAt = formData.get("expiresAt") ? `${formData.get("expiresAt")}T23:59:59Z` : null;

    createMutation.mutate({ title, body, pinned, expiresAt });
  };

  const handleAttachSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!attachTargetId) return;
    const formData = new FormData(e.currentTarget);
    attachMutation.mutate({ id: attachTargetId, formData });
  };

  if (isLoading) {
    return <LoadingState message="Loading notice board..." />;
  }

  return (
    <div className="space-y-6 max-w-6xl">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Notice Board Management</h1>
          <p className="text-sm text-slate-600 mt-1">
            Create school announcements, attach official PDF circulars, and publish notice updates.
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => setIsCreateOpen(true)}
          className="gap-2 shrink-0"
        >
          <Plus size={16} aria-hidden="true" />
          <span>New Notice</span>
        </Button>
      </div>

      <Card className="overflow-hidden p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-slate-700 font-bold">
                <th className="p-4">Notice Title</th>
                <th className="p-4">Attachment</th>
                <th className="p-4">Expiry Date</th>
                <th className="p-4">Status</th>
                <th className="p-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {notices.map((item) => (
                <tr key={item.id} className="hover:bg-slate-50/50">
                  <td className="p-4">
                    <div className="flex items-center gap-2 font-bold text-slate-900">
                      <span>{item.title}</span>
                      {item.pinned && (
                        <Badge variant="accent" size="sm" className="gap-1">
                          <Pin size={10} />
                          <span>PINNED</span>
                        </Badge>
                      )}
                    </div>
                    <div className="text-xs text-slate-500 mt-0.5 line-clamp-1 max-w-md">
                      {item.body}
                    </div>
                  </td>
                  <td className="p-4 text-xs font-mono text-slate-600">
                    {item.attachmentFilename ? (
                      <span className="text-blue-900 font-semibold flex items-center gap-1">
                        <Paperclip size={12} />
                        <span className="truncate max-w-[120px]">{item.attachmentFilename}</span>
                      </span>
                    ) : (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setAttachTargetId(item.id)}
                        className="text-xs text-slate-500 p-0 h-auto font-normal hover:text-blue-900"
                      >
                        + Attach File
                      </Button>
                    )}
                  </td>
                  <td className="p-4 text-xs text-slate-600">
                    {item.expiresAt ? item.expiresAt.slice(0, 10) : "No expiration"}
                  </td>
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
              {notices.length === 0 && (
                <tr>
                  <td colSpan={5} className="p-8 text-center text-slate-500">
                    No notices published. Click 'New Notice' to post one.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Create Notice Modal */}
      {isCreateOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-lg w-full shadow-2xl space-y-4">
            <h2 className="text-lg font-bold text-slate-900">Create New Announcement</h2>
            <form onSubmit={handleCreateSubmit} className="space-y-4">
              <Input label="Title" name="title" required placeholder="e.g. દિવાળી વેકેશન અંગે સૂચના" />
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Notice Details / Body <span className="text-red-500">*</span>
                </label>
                <textarea
                  name="body"
                  required
                  rows={4}
                  className="w-full rounded-lg border border-slate-300 p-2.5 text-sm text-slate-900"
                  placeholder="Notice announcement content..."
                />
              </div>
              <Input label="Expiration Date (Optional)" name="expiresAt" type="date" />
              <label className="flex items-center gap-2 text-sm text-slate-700 font-medium">
                <input type="checkbox" name="pinned" className="rounded text-blue-900 focus:ring-blue-900" />
                <span>Pin notice to top of notice board</span>
              </label>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="outline" size="sm" type="button" onClick={() => setIsCreateOpen(false)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" type="submit" loading={createMutation.isPending}>
                  Create Notice
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Attach File Modal */}
      {attachTargetId && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <h2 className="text-lg font-bold text-slate-900">Attach Document / PDF</h2>
            <form onSubmit={handleAttachSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Select Attachment File <span className="text-red-500">*</span>
                </label>
                <input
                  type="file"
                  name="file"
                  required
                  className="w-full text-xs text-slate-600 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:bg-blue-50 file:text-blue-900 file:font-semibold hover:file:bg-blue-100"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <Button variant="outline" size="sm" type="button" onClick={() => setAttachTargetId(null)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" type="submit" loading={attachMutation.isPending}>
                  Attach File
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
        title="Delete Notice?"
        description="Are you sure you want to delete this notice? This action cannot be undone."
        confirmText="Delete"
        variant="danger"
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}
