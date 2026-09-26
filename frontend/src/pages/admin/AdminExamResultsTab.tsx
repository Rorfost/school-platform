import { useState } from "react";
import { CheckCircle2, UploadCloud } from "lucide-react";
import { useMutation } from "@tanstack/react-query";
import { ApiError, apiRequest } from "@/api/client";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";

export function AdminExamResultsTab() {
  const [file, setFile] = useState<File | null>(null);
  const [totalWorkingDays, setTotalWorkingDays] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const uploadMutation = useMutation({
    mutationFn: () => {
      const formData = new FormData();
      formData.append("file", file!);
      formData.append("totalWorkingDays", totalWorkingDays);
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
      setErrorMessage(
        code === "annual_exam_format_invalid"
          ? "ફાઇલ વાર્ષિક પરિણામના માન્ય નમૂનામાં નથી. હેડર અને ગુણની કૉલમ તપાસો."
          : "અપલોડ નિષ્ફળ ગયું. ફાઇલ અને કાર્યદિવસ ફરી તપાસો.",
      );
      setSuccessMessage("");
    },
  });

  const submit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (file && totalWorkingDays) uploadMutation.mutate();
  };

  return (
    <Card className="max-w-2xl">
      <h2 className="text-lg font-bold text-slate-900">વાર્ષિક પરીક્ષા પરિણામ અપલોડ કરો</h2>
      <p className="mt-1 text-sm text-slate-600">વાર્ષિક પરીક્ષાની Excel ફાઇલ અને કુલ કાર્યદિવસ દાખલ કરો.</p>

      <form onSubmit={submit} className="mt-5 space-y-4">
        <Input
          label="કુલ કાર્યદિવસ"
          type="number"
          min="1"
          value={totalWorkingDays}
          onChange={(event) => setTotalWorkingDays(event.target.value)}
          placeholder="દા.ત. 230"
          required
        />
        <label className="block text-sm font-medium text-slate-700">
          Excel ફાઇલ
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
        {errorMessage && <p className="rounded-lg bg-red-50 p-3 text-sm font-medium text-red-700">{errorMessage}</p>}

        <Button
          type="submit"
          className="gap-2"
          disabled={!file || !totalWorkingDays}
          loading={uploadMutation.isPending}
        >
          <UploadCloud size={18} aria-hidden="true" /> પરિણામ અપલોડ કરો
        </Button>
      </form>
    </Card>
  );
}
