import { useState } from "react";
import { CheckCircle2, UploadCloud } from "lucide-react";
import { useMutation } from "@tanstack/react-query";
import { ApiError, apiRequest } from "@/api/client";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";

type ResultType = "ANNUAL" | "EKAM_KASOTI";

const RESULT_TYPE_LABELS: Record<ResultType, string> = {
  ANNUAL: "Exam Result",
  EKAM_KASOTI: "Ekam Kasoti Result",
};

export function AdminExamResultsTab() {
  const [resultType, setResultType] = useState<ResultType>("ANNUAL");
  const [file, setFile] = useState<File | null>(null);
  const [totalWorkingDays, setTotalWorkingDays] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const uploadMutation = useMutation({
    mutationFn: () => {
      const formData = new FormData();
      formData.append("file", file!);
      formData.append("totalWorkingDays", totalWorkingDays);
      formData.append("resultType", resultType);
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
      if (reason instanceof ApiError && reason.status === 404) {
        setErrorMessage(
          "The result-upload service is unavailable. Deploy the latest backend, then try again.",
        );
      } else if (code === "exam_result_format_invalid") {
        setErrorMessage(
          "This file does not match the required result workbook format. Check the header and marks columns.",
        );
      } else {
        setErrorMessage("Upload failed. Check the file and total working days, then try again.");
      }
      setSuccessMessage("");
    },
  });

  const submit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (file && totalWorkingDays) uploadMutation.mutate();
  };

  return (
    <Card className="max-w-2xl">
      <h2 className="text-lg font-bold text-slate-900">Upload Result Workbook</h2>
      <p className="mt-1 text-sm text-slate-600">
        Exam Result and Ekam Kasoti Result use the same approved Excel workbook format.
      </p>

      <form onSubmit={submit} className="mt-5 space-y-4">
        <label className="block text-sm font-medium text-slate-700">
          Result type
          <select
            value={resultType}
            onChange={(event) => {
              setResultType(event.target.value as ResultType);
              setSuccessMessage("");
              setErrorMessage("");
            }}
            className="mt-1 block min-h-11 w-full rounded-lg border border-slate-300 bg-white px-3"
          >
            <option value="ANNUAL">{RESULT_TYPE_LABELS.ANNUAL}</option>
            <option value="EKAM_KASOTI">{RESULT_TYPE_LABELS.EKAM_KASOTI}</option>
          </select>
        </label>

        <Input
          label="Total working days"
          type="number"
          min="1"
          value={totalWorkingDays}
          onChange={(event) => setTotalWorkingDays(event.target.value)}
          placeholder="For example, 230"
          required
        />

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
          disabled={!file || !totalWorkingDays}
          loading={uploadMutation.isPending}
          loadingText="Uploading result..."
        >
          <UploadCloud size={18} aria-hidden="true" /> Upload {RESULT_TYPE_LABELS[resultType]}
        </Button>
      </form>
    </Card>
  );
}
