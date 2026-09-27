import { useState } from "react";
import { CheckCircle2, UploadCloud } from "lucide-react";
import { useMutation } from "@tanstack/react-query";
import { ApiError, apiRequest } from "@/api/client";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import type { ResultSettingsTab } from "./AdminAssessmentsPage";

const RESULT_TYPE_LABELS: Record<ResultSettingsTab, string> = {
  ANNUAL: "Exam Result",
  EKAM_KASOTI: "Ekam Kasoti Result",
};

export function AdminExamResultsTab({ resultType }: { resultType: ResultSettingsTab }) {
  const isAnnual = resultType === "ANNUAL";
  const [file, setFile] = useState<File | null>(null);
  const [totalWorkingDays, setTotalWorkingDays] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const uploadMutation = useMutation({
    mutationFn: () => {
      const formData = new FormData();
      formData.append("file", file!);
      formData.append("resultType", resultType);
      if (isAnnual) {
        formData.append("totalWorkingDays", totalWorkingDays);
      }
      return apiRequest<{ message: string }>("/api/v1/admin/exam-results/upload", {
        method: "POST",
        body: formData,
      });
    },
    onSuccess: (response) => {
      setSuccessMessage(response.message);
      setErrorMessage("");
      setFile(null);
      setTotalWorkingDays("");
    },
    onError: (reason) => {
      const code = reason instanceof ApiError ? reason.code : undefined;
      if (code === "school_not_configured") {
        setErrorMessage("School details must be configured before results can be uploaded.");
      } else if (reason instanceof ApiError && reason.status === 404) {
        setErrorMessage(
          "The result-upload service is unavailable. Deploy the latest backend, then try again.",
        );
      } else if (code === "exam_result_format_invalid") {
        setErrorMessage(
          "This file does not match the required result workbook format. Check the header and marks columns.",
        );
      } else if (code === "result_standard_not_configured") {
        setErrorMessage(
          "Set up the Standard in Academic Setup first. Its name or number must match the Standard column in this workbook.",
        );
      } else if (code === "result_subject_maximums_not_configured") {
        setErrorMessage(
          "Open Academic Setup, choose this Standard's Maximum Marks, and save a maximum for every selected Subject before uploading.",
        );
      } else {
        setErrorMessage(
          isAnnual
            ? "Upload failed. Check the file and total working days, then try again."
            : "Upload failed. Check the file, then try again.",
        );
      }
      setSuccessMessage("");
    },
  });

  const submit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!file) return;
    if (isAnnual && !totalWorkingDays) return;
    uploadMutation.mutate();
  };

  const canSubmit = Boolean(file) && (!isAnnual || Boolean(totalWorkingDays));

  return (
    <Card className="max-w-2xl">
      <h2 className="text-lg font-bold text-slate-900">
        Upload {RESULT_TYPE_LABELS[resultType]} Workbook
      </h2>
      <p className="mt-1 text-sm text-slate-600">
        {isAnnual
          ? "Exam Result workbooks start with G.R. No., Standard, Name, Birth Date, and Hajar Divas, then subject mark and grade pairs. There is no Sr.No. column."
          : "Ekam Kasoti workbooks start with G.R. No., Standard, Name, and Birth Date, then subject mark and grade pairs. There is no Sr.No. column and no Hajar Divas column."}
      </p>
      <p className="mt-2 rounded-lg border border-blue-100 bg-blue-50 p-3 text-sm text-blue-950">
        Before the first upload, set the Standard, its Subjects, and each Subject&apos;s maximum
        marks in Academic Setup. Those saved values are used to calculate the correct percentage.
      </p>

      <form onSubmit={submit} className="mt-5 space-y-4">
        {isAnnual && (
          <Input
            label="Total working days"
            type="number"
            min="1"
            value={totalWorkingDays}
            onChange={(event) => setTotalWorkingDays(event.target.value)}
            placeholder="For example, 230"
            required
          />
        )}

        <label className="block text-sm font-medium text-slate-700">
          Excel workbook
          <input
            type="file"
            accept=".xlsx,.xls,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel"
            required
            onChange={(event) => {
              setFile(event.target.files?.[0] ?? null);
              setSuccessMessage("");
              setErrorMessage("");
            }}
            className="mt-1.5 block w-full rounded-lg border border-slate-300 bg-white text-sm text-slate-600 file:mr-4 file:border-0 file:bg-blue-50 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-[#0d2461] hover:file:bg-blue-100"
          />
        </label>

        {successMessage && (
          <p className="flex items-center gap-2 rounded-lg bg-emerald-50 p-3 text-sm font-semibold text-emerald-800">
            <CheckCircle2 size={18} aria-hidden="true" /> {successMessage}
          </p>
        )}
        {errorMessage && (
          <p className="rounded-lg bg-red-50 p-3 text-sm font-medium text-red-700">
            {errorMessage}
          </p>
        )}

        <Button
          type="submit"
          className="gap-2"
          disabled={!canSubmit}
          loading={uploadMutation.isPending}
          loadingText="Uploading result..."
        >
          <UploadCloud size={18} aria-hidden="true" /> Upload {RESULT_TYPE_LABELS[resultType]}
        </Button>
      </form>
    </Card>
  );
}
