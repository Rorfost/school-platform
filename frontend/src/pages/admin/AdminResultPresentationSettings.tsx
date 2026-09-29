import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Save } from "lucide-react";
import { apiRequest } from "@/api/client";
import type { ResultPresentationSettingsResponse } from "@/api/types";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import type { ResultSettingsTab } from "./AdminAssessmentsPage";

export function AdminResultPresentationSettings({
  settingsTab,
}: {
  settingsTab: ResultSettingsTab;
}) {
  const queryClient = useQueryClient();
  const settings = useQuery<ResultPresentationSettingsResponse>({
    queryKey: ["admin", "result-settings"],
    queryFn: () => apiRequest<ResultPresentationSettingsResponse>("/api/v1/admin/result-settings"),
  });
  const save = useMutation({
    mutationFn: (payload: ResultPresentationSettingsResponse) =>
      apiRequest<ResultPresentationSettingsResponse>("/api/v1/admin/result-settings", {
        method: "PUT",
        body: payload,
      }),
    onSuccess: (value) => queryClient.setQueryData(["admin", "result-settings"], value),
  });
  if (!settings.data) return null;
  const value = settings.data;
  const isAnnual = settingsTab === "ANNUAL";

  return (
    <Card className="max-w-2xl">
      <h2 className="text-lg font-bold text-slate-900">
        {isAnnual ? "Exam Result Sheet Settings" : "Trimasik Kasoti Result Sheet Settings"}
      </h2>
      <form
        key={settingsTab}
        className="mt-4 space-y-4"
        onSubmit={(event) => {
          event.preventDefault();
          const form = new FormData(event.currentTarget);
          save.mutate({
            resultDate: isAnnual
              ? String(form.get("resultDate") || "") || null
              : (value.resultDate ?? null),
            resultSheetTitle: isAnnual
              ? String(form.get("resultSheetTitle") || "") || null
              : (value.resultSheetTitle ?? null),
            footerLineOne: isAnnual
              ? String(form.get("footerLineOne") || "") || null
              : (value.footerLineOne ?? null),
            footerLineTwo: isAnnual
              ? String(form.get("footerLineTwo") || "") || null
              : (value.footerLineTwo ?? null),
            ekamResultDate: isAnnual
              ? (value.ekamResultDate ?? null)
              : String(form.get("ekamResultDate") || "") || null,
            ekamResultSheetTitle: isAnnual
              ? (value.ekamResultSheetTitle ?? null)
              : String(form.get("ekamResultSheetTitle") || "") || null,
            ekamFooterLineOne: isAnnual
              ? (value.ekamFooterLineOne ?? null)
              : String(form.get("ekamFooterLineOne") || "") || null,
            ekamFooterLineTwo: isAnnual
              ? (value.ekamFooterLineTwo ?? null)
              : String(form.get("ekamFooterLineTwo") || "") || null,
            gradeAMin: isAnnual ? Number(form.get("gradeAMin")) : value.gradeAMin,
            gradeBMin: isAnnual ? Number(form.get("gradeBMin")) : value.gradeBMin,
            gradeCMin: isAnnual ? Number(form.get("gradeCMin")) : value.gradeCMin,
            gradeDMin: isAnnual ? Number(form.get("gradeDMin")) : value.gradeDMin,
          });
        }}
      >
        {isAnnual ? (
          <>
            <Input
              label="Result date"
              name="resultDate"
              type="date"
              defaultValue={value.resultDate ?? ""}
            />
            <Input
              label="Result sheet title (e.g. પરીક્ષા પરિણામ પત્રક : ૨૦૨૫-૨૬)"
              name="resultSheetTitle"
              defaultValue={value.resultSheetTitle ?? ""}
              placeholder="પરીક્ષા પરિણામ પત્રક : ૨૦૨૫-૨૬"
            />
            <div className="space-y-3 border-t border-slate-100 pt-3">
              <h3 className="text-sm font-bold text-slate-800">Footer notes</h3>
              <label className="block text-sm font-medium text-slate-700">
                Footer line 1
                <textarea
                  name="footerLineOne"
                  defaultValue={value.footerLineOne ?? ""}
                  className="mt-1 w-full rounded-lg border border-slate-300 p-2 text-sm"
                  rows={2}
                />
              </label>
              <label className="block text-sm font-medium text-slate-700">
                Footer line 2
                <textarea
                  name="footerLineTwo"
                  defaultValue={value.footerLineTwo ?? ""}
                  className="mt-1 w-full rounded-lg border border-slate-300 p-2 text-sm"
                  rows={2}
                />
              </label>
            </div>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 border-t border-slate-100 pt-3">
              <Input
                label="A minimum %"
                name="gradeAMin"
                type="number"
                defaultValue={value.gradeAMin}
                required
              />
              <Input
                label="B minimum %"
                name="gradeBMin"
                type="number"
                defaultValue={value.gradeBMin}
                required
              />
              <Input
                label="C minimum %"
                name="gradeCMin"
                type="number"
                defaultValue={value.gradeCMin}
                required
              />
              <Input
                label="D minimum %"
                name="gradeDMin"
                type="number"
                defaultValue={value.gradeDMin}
                required
              />
            </div>
          </>
        ) : (
          <>
            <Input
              label="Result date"
              name="ekamResultDate"
              type="date"
              defaultValue={value.ekamResultDate ?? ""}
            />
            <Input
              label="Result sheet title (e.g. ત્રિમાસિક કસોટી પરિણામ પત્રક : ૨૦૨૫-૨૬)"
              name="ekamResultSheetTitle"
              defaultValue={value.ekamResultSheetTitle ?? ""}
              placeholder="ત્રિમાસિક કસોટી પરિણામ પત્રક : ૨૦૨૫-૨૬"
            />
            <div className="space-y-3 border-t border-slate-100 pt-3">
              <h3 className="text-sm font-bold text-slate-800">Footer notes</h3>
              <label className="block text-sm font-medium text-slate-700">
                Footer line 1
                <textarea
                  name="ekamFooterLineOne"
                  defaultValue={value.ekamFooterLineOne ?? ""}
                  className="mt-1 w-full rounded-lg border border-slate-300 p-2 text-sm"
                  rows={2}
                />
              </label>
              <label className="block text-sm font-medium text-slate-700">
                Footer line 2
                <textarea
                  name="ekamFooterLineTwo"
                  defaultValue={value.ekamFooterLineTwo ?? ""}
                  className="mt-1 w-full rounded-lg border border-slate-300 p-2 text-sm"
                  rows={2}
                />
              </label>
            </div>
          </>
        )}
        <Button type="submit" loading={save.isPending} loadingText="Saving result settings...">
          <Save size={16} /> Save Result Settings
        </Button>
      </form>
    </Card>
  );
}
