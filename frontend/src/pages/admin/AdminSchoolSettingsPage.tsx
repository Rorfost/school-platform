import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Save, School as SchoolIcon } from "lucide-react";
import { ApiError, apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { SchoolResponse, SchoolUpdateRequest } from "@/api/types";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { LoadingState } from "@/components/common/StatusPanel";

export function AdminSchoolSettingsPage() {
  const queryClient = useQueryClient();
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const { data: school, isLoading } = useQuery<SchoolResponse>({
    queryKey: queryKeys.school,
    queryFn: () => apiRequest<SchoolResponse>("/api/v1/admin/school"),
  });

  const updateMutation = useMutation({
    mutationFn: (payload: SchoolUpdateRequest) =>
      apiRequest<SchoolResponse>("/api/v1/admin/school", {
        method: "PUT",
        body: payload,
      }),
    onSuccess: (updated) => {
      // Invalidate so public portal header/contact page reflects updated school name
      queryClient.setQueryData(queryKeys.school, updated);
      queryClient.invalidateQueries({ queryKey: queryKeys.school });
      setSuccessMessage("School identity settings updated successfully.");
      setTimeout(() => setSuccessMessage(null), 4000);
    },
  });

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const payload: SchoolUpdateRequest = {
      name: String(formData.get("name") || ""),
      shortName: String(formData.get("shortName") || "") || null,
      schoolCode: String(formData.get("schoolCode") || "") || null,
      address: String(formData.get("address") || "") || null,
      city: String(formData.get("city") || "") || null,
      state: String(formData.get("state") || "") || null,
      postalCode: String(formData.get("postalCode") || "") || null,
      email: String(formData.get("email") || "") || null,
      phone: String(formData.get("phone") || "") || null,
      website: String(formData.get("website") || "") || null,
      mapsUrl: String(formData.get("mapsUrl") || "") || null,
      about: String(formData.get("about") || "") || null,
      establishedYear: formData.get("establishedYear")
        ? Number(formData.get("establishedYear"))
        : null,
      medium: String(formData.get("medium") || "") || null,
      schoolType: String(formData.get("schoolType") || "") || null,
      logoObjectKey: school?.logoObjectKey ?? null,
    };

    updateMutation.mutate(payload);
  };

  const errorMessage =
    updateMutation.error instanceof ApiError
      ? updateMutation.error.message
      : updateMutation.isError
        ? "Failed to update school settings."
        : null;

  if (isLoading) {
    return <LoadingState message="Loading school settings..." />;
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">School Identity & Settings</h1>
        <p className="text-sm text-slate-600 mt-1">
          Manage official school name, DISE code, location, contact, and branding displayed on the
          public website.
        </p>
      </div>

      {successMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-900 text-sm font-semibold flex items-center gap-2">
          <CheckCircle2 size={18} className="text-emerald-600 shrink-0" />
          <span>{successMessage}</span>
        </div>
      )}

      {errorMessage && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-900 text-sm font-medium">
          {errorMessage}
        </div>
      )}

      {/* key forces form to re-mount with fresh defaultValues once backend data arrives */}
      <form key={school?.id ?? "loading"} onSubmit={handleSubmit} className="space-y-6">
        {/* Basic Identity */}
        <Card className="p-6 space-y-4">
          <h2 className="text-base font-bold text-slate-900 border-b border-slate-100 pb-3 flex items-center gap-2">
            <SchoolIcon size={18} className="text-blue-900" />
            <span>Official Identity & Code</span>
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Official School Name"
              name="name"
              required
              defaultValue={school?.name ?? ""}
            />
            <Input label="Short Name" name="shortName" defaultValue={school?.shortName ?? ""} />
            <Input label="DISE Code" name="schoolCode" defaultValue={school?.schoolCode ?? ""} />
            <Input
              label="Established Year"
              name="establishedYear"
              type="number"
              defaultValue={school?.establishedYear ?? ""}
            />
            <Input
              label="Medium of Instruction"
              name="medium"
              defaultValue={school?.medium ?? ""}
              placeholder="e.g. Gujarati"
            />
            <Input
              label="School Category / Type"
              name="schoolType"
              defaultValue={school?.schoolType ?? ""}
              placeholder="e.g. Primary (Std 1–8)"
            />
          </div>
        </Card>

        {/* Address & Location */}
        <Card className="p-6 space-y-4">
          <h2 className="text-base font-bold text-slate-900 border-b border-slate-100 pb-3">
            Address & Location
          </h2>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">Full Address</label>
            <textarea
              name="address"
              rows={2}
              className="w-full rounded-lg border border-slate-300 p-2.5 text-sm text-slate-900 focus:border-blue-900 focus:outline-none focus:ring-1 focus:ring-blue-900"
              defaultValue={school?.address ?? ""}
            />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <Input label="Village / City" name="city" defaultValue={school?.city ?? ""} />
            <Input label="State" name="state" defaultValue={school?.state ?? ""} />
            <Input
              label="Postal Code / PIN"
              name="postalCode"
              defaultValue={school?.postalCode ?? ""}
            />
          </div>
          <Input
            label="Google Maps Embedded Link / URL"
            name="mapsUrl"
            defaultValue={school?.mapsUrl ?? ""}
            placeholder="https://maps.google.com/..."
          />
        </Card>

        {/* Contact & Web */}
        <Card className="p-6 space-y-4">
          <h2 className="text-base font-bold text-slate-900 border-b border-slate-100 pb-3">
            Contact Details
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Official Email"
              name="email"
              type="email"
              defaultValue={school?.email ?? ""}
            />
            <Input
              label="Contact Phone"
              name="phone"
              defaultValue={school?.phone ?? ""}
              placeholder="e.g. +91 9876543210"
            />
            <Input
              label="Website URL"
              name="website"
              defaultValue={school?.website ?? ""}
              placeholder="https://..."
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">
              About School Description
            </label>
            <textarea
              name="about"
              rows={4}
              className="w-full rounded-lg border border-slate-300 p-2.5 text-sm text-slate-900 focus:border-blue-900 focus:outline-none focus:ring-1 focus:ring-blue-900"
              defaultValue={school?.about ?? ""}
            />
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
            <span>Save School Settings</span>
          </Button>
        </div>
      </form>
    </div>
  );
}
