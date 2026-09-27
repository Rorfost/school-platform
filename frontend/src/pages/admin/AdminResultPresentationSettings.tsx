import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Save } from "lucide-react";
import { apiRequest } from "@/api/client";
import type { ResultPresentationSettingsResponse } from "@/api/types";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";

export function AdminResultPresentationSettings() {
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
  return (
    <Card className="max-w-2xl">
      <h2 className="text-lg font-bold text-slate-900">Result Sheet Settings</h2>
      <form
        className="mt-4 space-y-4"
        onSubmit={(event) => {
          event.preventDefault();
          const form = new FormData(event.currentTarget);
          save.mutate({
            resultDate: String(form.get("resultDate") || "") || null,
            resultSheetTitle: String(form.get("resultSheetTitle") || "") || null,
            footerLineOne: String(form.get("footerLineOne") || "") || null,
            footerLineTwo: String(form.get("footerLineTwo") || "") || null,
            ekamFooterLineOne: String(form.get("ekamFooterLineOne") || "") || null,
            ekamFooterLineTwo: String(form.get("ekamFooterLineTwo") || "") || null,
            gradeAMin: Number(form.get("gradeAMin")),
            gradeBMin: Number(form.get("gradeBMin")),
            gradeCMin: Number(form.get("gradeCMin")),
            gradeDMin: Number(form.get("gradeDMin")),
          });
        }}
      >
        <Input
          label="Result date"
          name="resultDate"
          type="date"
          defaultValue={value.resultDate ?? ""}
        />
        <Input
          label="Result sheet title (e.g. પરિણામ પત્રક : ૨૦૨૫-૨૬)"
          name="resultSheetTitle"
          defaultValue={value.resultSheetTitle ?? ""}
          placeholder="પરિણામ પત્રક : ૨૦૨૫-૨૬"
        />
        <div className="space-y-3 border-t border-slate-100 pt-3">
          <h3 className="text-sm font-bold text-slate-800">Annual Exam Footer Notes</h3>
          <label className="block text-sm font-medium text-slate-700">
            Exam Footer line 1 (Default: Summer Vacation reopening note)
            <textarea
              name="footerLineOne"
              defaultValue={value.footerLineOne ?? ""}
              className="mt-1 w-full rounded-lg border border-slate-300 p-2 text-sm"
              rows={2}
            />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            Exam Footer line 2 (Default: Grade scale explanation)
            <textarea
              name="footerLineTwo"
              defaultValue={value.footerLineTwo ?? ""}
              className="mt-1 w-full rounded-lg border border-slate-300 p-2 text-sm"
              rows={2}
            />
          </label>
        </div>

        <div className="space-y-3 border-t border-slate-100 pt-3">
          <h3 className="text-sm font-bold text-slate-800">Ekam Kasoti Footer Notes</h3>
          <label className="block text-sm font-medium text-slate-700">
            Ekam Kasoti Footer line 1 (Default: Parent signature return request)
            <textarea
              name="ekamFooterLineOne"
              defaultValue={value.ekamFooterLineOne ?? ""}
              className="mt-1 w-full rounded-lg border border-slate-300 p-2 text-sm"
              rows={2}
            />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            Ekam Kasoti Footer line 2 (Default: Grade scale explanation)
            <textarea
              name="ekamFooterLineTwo"
              defaultValue={value.ekamFooterLineTwo ?? ""}
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
        <Button type="submit" loading={save.isPending} loadingText="Saving result settings...">
          <Save size={16} /> Save Result Settings
        </Button>
      </form>
    </Card>
  );
}
