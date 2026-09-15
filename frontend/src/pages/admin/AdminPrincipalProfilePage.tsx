import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Save, UserCheck } from "lucide-react";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { PrincipalProfileResponse, PrincipalProfileUpdateRequest } from "@/api/types";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { LoadingState } from "@/components/common/StatusPanel";

export function AdminPrincipalProfilePage() {
  const queryClient = useQueryClient();
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const { data: profile, isLoading } = useQuery<PrincipalProfileResponse>({
    queryKey: queryKeys.principalProfile,
    queryFn: () => apiRequest<PrincipalProfileResponse>("/api/v1/admin/principal-profile"),
  });

  const updateMutation = useMutation({
    mutationFn: (payload: PrincipalProfileUpdateRequest) =>
      apiRequest<PrincipalProfileResponse>("/api/v1/admin/principal-profile", {
        method: "PUT",
        body: payload,
      }),
    onSuccess: (updated) => {
      queryClient.setQueryData(queryKeys.principalProfile, updated);
      setSuccessMsg("Principal profile updated successfully.");
      setTimeout(() => setSuccessMsg(null), 4000);
    },
  });

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const payload: PrincipalProfileUpdateRequest = {
      fullName: String(formData.get("fullName") || ""),
      designation: String(formData.get("designation") || "") || null,
      qualification: String(formData.get("qualification") || "") || null,
      message: String(formData.get("message") || "") || null,
      biography: String(formData.get("biography") || "") || null,
      email: String(formData.get("email") || "") || null,
      phone: String(formData.get("phone") || "") || null,
      isPublic: formData.get("isPublic") === "on",
      isContactPublic: formData.get("isContactPublic") === "on",
      portraitObjectKey: profile?.portraitObjectKey ?? null,
    };

    updateMutation.mutate(payload);
  };

  if (isLoading) {
    return <LoadingState message="Loading Principal profile..." />;
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">Principal Profile & Message</h1>
        <p className="text-sm text-slate-600 mt-1">
          Manage the Principal's message, designation, qualification, and public desk visibility
          settings.
        </p>
      </div>

      {successMsg && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-900 text-sm font-semibold flex items-center gap-2">
          <CheckCircle2 size={18} className="text-emerald-600 shrink-0" />
          <span>{successMsg}</span>
        </div>
      )}

      {updateMutation.isError && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-900 text-sm font-medium">
          Failed to update profile.{" "}
          {updateMutation.error instanceof Error ? updateMutation.error.message : ""}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Personal & Professional Info */}
        <Card className="p-6 space-y-4">
          <h2 className="text-base font-bold text-slate-900 border-b border-slate-100 pb-3 flex items-center gap-2">
            <UserCheck size={18} className="text-blue-900" />
            <span>Identity & Designation</span>
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Full Name"
              name="fullName"
              required
              defaultValue={profile?.fullName || "આચાર્યશ્રી"}
            />
            <Input
              label="Designation"
              name="designation"
              defaultValue={profile?.designation || "મુખ્ય શિક્ષક / આચાર્ય"}
            />
            <Input
              label="Qualifications"
              name="qualification"
              defaultValue={profile?.qualification || "M.A., B.Ed."}
              placeholder="e.g. M.A., B.Ed."
            />
            <Input
              label="Contact Email"
              name="email"
              type="email"
              defaultValue={profile?.email || "principal24030401801@ssguj.in"}
            />
            <Input
              label="Contact Phone"
              name="phone"
              defaultValue={profile?.phone || ""}
              placeholder="e.g. +91 9876543210"
            />
          </div>
        </Card>

        {/* Message to Students & Parents */}
        <Card className="p-6 space-y-4">
          <h2 className="text-base font-bold text-slate-900 border-b border-slate-100 pb-3">
            Principal's Desk Message (Gujarati / English)
          </h2>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">
              Public Message
            </label>
            <textarea
              name="message"
              rows={6}
              className="w-full rounded-lg border border-slate-300 p-2.5 text-sm text-slate-900 focus:border-blue-900 focus:outline-none focus:ring-1 focus:ring-blue-900"
              defaultValue={
                profile?.message ||
                "પ્રિય વિદ્યાર્થીઓ અને વાલીશ્રીઓ,\n\nઅમારી શાળામાં આપ સૌનું હાર્દિક સ્વાગત છે."
              }
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">
              Biography & Background
            </label>
            <textarea
              name="biography"
              rows={3}
              className="w-full rounded-lg border border-slate-300 p-2.5 text-sm text-slate-900 focus:border-blue-900 focus:outline-none focus:ring-1 focus:ring-blue-900"
              defaultValue={profile?.biography || ""}
              placeholder="Educational background and service journey..."
            />
          </div>
        </Card>

        {/* Visibility Switches */}
        <Card className="p-6 space-y-4">
          <h2 className="text-base font-bold text-slate-900 border-b border-slate-100 pb-3">
            Public Visibility Options
          </h2>
          <div className="space-y-3">
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                name="isPublic"
                defaultChecked={profile?.isPublic ?? true}
                className="size-4 rounded text-blue-900 focus:ring-blue-900 border-slate-300"
              />
              <span className="text-sm font-medium text-slate-800">
                Show Principal's Desk section on public website
              </span>
            </label>
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                name="isContactPublic"
                defaultChecked={profile?.isContactPublic ?? true}
                className="size-4 rounded text-blue-900 focus:ring-blue-900 border-slate-300"
              />
              <span className="text-sm font-medium text-slate-800">
                Display Principal's email & phone on public profile
              </span>
            </label>
          </div>
        </Card>

        <div className="flex justify-end">
          <Button
            type="submit"
            variant="primary"
            className="gap-2 px-6"
            loading={updateMutation.isPending}
          >
            <Save size={16} aria-hidden="true" />
            <span>Save Profile</span>
          </Button>
        </div>
      </form>
    </div>
  );
}
