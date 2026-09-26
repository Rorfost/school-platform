import { useState } from "react";
import { CheckCircle2, UploadCloud } from "lucide-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ApiError, apiRequest } from "@/api/client";
import type { AcademicYearResponse, StandardResponse, StudentRequest } from "@/api/types";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";

const ERROR_MESSAGES: Record<string, string> = {
  ekam_kasoti_format_invalid: "આ ફાઇલ એકમ કસોટીના માન્ય નમૂનામાં નથી.",
  ekam_kasoti_students_not_enrolled:
    "અપલોડ પહેલાં આ ધોરણના વિદ્યાર્થીઓને રોલ નંબર સાથે Academic Setup માં દાખલ કરો.",
  ekam_kasoti_duplicate_student_name:
    "વિદ્યાર્થીની યાદીમાં એકસરખા નામ છે. દરેક વિદ્યાર્થી માટે અલગ નામ અથવા રોલ નંબર ગોઠવો.",
};

export function AdminEkamKasotiTab() {
  const [file, setFile] = useState<File | null>(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [rosterMessage, setRosterMessage] = useState("");
  const yearsQuery = useQuery({
    queryKey: ["admin", "academic-years"],
    queryFn: () => apiRequest<AcademicYearResponse[]>("/api/v1/admin/academic-years"),
  });
  const standardsQuery = useQuery({
    queryKey: ["admin", "standards"],
    queryFn: () => apiRequest<StandardResponse[]>("/api/v1/admin/standards"),
  });
  const currentYear = yearsQuery.data?.find((year) => year.status === "CURRENT");
  const standardThree = standardsQuery.data?.find(
    (standard) => standard.code === "3" || /(^|\D)3(\D|$)/.test(standard.displayName),
  );

  const uploadMutation = useMutation({
    mutationFn: () => {
      const formData = new FormData();
      formData.append("file", file!);
      return apiRequest<{ message: string }>("/api/v1/admin/exam-results/ekam-kasoti/upload", {
        method: "POST",
        body: formData,
      });
    },
    onSuccess: (response) => {
      setMessage(response.message);
      setError("");
      setFile(null);
    },
    onError: (reason) => {
      const code = reason instanceof ApiError ? reason.code : undefined;
      setError(ERROR_MESSAGES[code ?? ""] ?? "અપલોડ થઈ શક્યો નથી. ફાઇલ ફરી તપાસો.");
      setMessage("");
    },
  });
  const rosterMutation = useMutation({
    mutationFn: (payload: StudentRequest) =>
      apiRequest("/api/v1/admin/academic/students", { method: "POST", body: payload }),
    onSuccess: () => setRosterMessage("વિદ્યાર્થી સફળતાપૂર્વક ઉમેરાયો."),
    onError: () => setRosterMessage("વિદ્યાર્થી ઉમેરાઈ શક્યો નથી. નામ અને રોલ નંબર તપાસો."),
  });

  const submit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (file) uploadMutation.mutate();
  };

  const addRosterStudent = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!currentYear || !standardThree) return;
    const values = new FormData(event.currentTarget);
    rosterMutation.mutate({
      academicYearId: currentYear.id,
      standardId: standardThree.id,
      fullName: String(values.get("fullName") ?? "").trim(),
      rollNumber: String(values.get("rollNumber") ?? "").trim(),
      isArchived: false,
    });
    event.currentTarget.reset();
  };

  return (
    <Card className="max-w-2xl">
      <h2 className="text-lg font-bold text-slate-900">એકમ કસોટી પરિણામ અપલોડ કરો</h2>
      <p className="mt-1 text-sm leading-relaxed text-slate-600">
        ધોરણ 3 માટેની એકમ કસોટીની Excel (.xlsx) ફાઇલ અપલોડ કરો. Aadhaar નંબરનો ઉપયોગ કે સંગ્રહ થતો
        નથી; પરિણામ સુરક્ષિત રીતે પહેલેથી દાખલ કરેલા વિદ્યાર્થીના રોલ નંબર સાથે જોડાય છે.
      </p>

      <form onSubmit={submit} className="mt-5 space-y-4">
        <label className="block text-sm font-medium text-slate-700">
          Excel ફાઇલ
          <input
            type="file"
            accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            required
            onChange={(event) => {
              setFile(event.target.files?.[0] ?? null);
              setError("");
              setMessage("");
            }}
            className="mt-1.5 block w-full rounded-lg border border-slate-300 bg-white text-sm text-slate-600 file:mr-4 file:border-0 file:bg-blue-50 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-[#0d2461] hover:file:bg-blue-100"
          />
        </label>

        {message && (
          <p className="flex items-center gap-2 rounded-lg bg-emerald-50 p-3 text-sm font-semibold text-emerald-800">
            <CheckCircle2 size={18} aria-hidden="true" /> {message}
          </p>
        )}
        {error && (
          <p className="rounded-lg bg-red-50 p-3 text-sm font-medium text-red-700">{error}</p>
        )}

        <Button type="submit" disabled={!file} loading={uploadMutation.isPending} className="gap-2">
          <UploadCloud size={18} aria-hidden="true" /> પરિણામ અપલોડ કરો
        </Button>
      </form>

      <div className="mt-6 border-t border-slate-200 pt-5">
        <h3 className="font-semibold text-slate-900">વિદ્યાર્થી રોલ નંબર યાદી</h3>
        <p className="mt-1 text-sm text-slate-600">
          પ્રથમ વખત અપલોડ કરતાં પહેલાં દરેક વિદ્યાર્થીને શાળાનો રોલ નંબર આપો. Excel માંથી Aadhaar
          નંબર લેવાતો નથી.
        </p>
        {!currentYear || !standardThree ? (
          <p className="mt-3 rounded-lg bg-amber-50 p-3 text-sm text-amber-800">
            પહેલાં Academic Setup માં વર્તમાન શૈક્ષણિક વર્ષ અને ધોરણ 3 ગોઠવો.
          </p>
        ) : (
          <form
            onSubmit={addRosterStudent}
            className="mt-3 grid gap-3 sm:grid-cols-[1fr_9rem_auto] sm:items-end"
          >
            <label className="text-sm font-medium text-slate-700">
              વિદ્યાર્થીનું નામ
              <input
                name="fullName"
                required
                className="mt-1 block min-h-11 w-full rounded-lg border border-slate-300 px-3"
              />
            </label>
            <label className="text-sm font-medium text-slate-700">
              રોલ નંબર
              <input
                name="rollNumber"
                inputMode="numeric"
                pattern="[0-9]+"
                required
                className="mt-1 block min-h-11 w-full rounded-lg border border-slate-300 px-3"
              />
            </label>
            <Button type="submit" loading={rosterMutation.isPending}>
              ઉમેરો
            </Button>
          </form>
        )}
        {rosterMessage && (
          <p className="mt-3 text-sm font-medium text-slate-700">{rosterMessage}</p>
        )}
      </div>
    </Card>
  );
}
