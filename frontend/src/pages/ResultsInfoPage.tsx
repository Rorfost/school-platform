import { useState } from "react";
import { Search } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { apiRequest, ApiError } from "@/api/client";
import type { ExamResultResponse } from "@/api/types";
import { ExamResultViewer } from "@/features/public/ExamResultViewer";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";

export function ResultsInfoPage() {
  const [resultType, setResultType] = useState<"ANNUAL" | "EKAM_KASOTI">("ANNUAL");
  const [standard, setStandard] = useState("");
  const [rollNumber, setRollNumber] = useState("");
  const [result, setResult] = useState<ExamResultResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const school = useEffectiveSchoolInfo();
  const isStandardValid = /^[1-8]$/.test(standard);
  const isRollNumberValid = /^[1-9]\d*$/.test(rollNumber);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isStandardValid || !isRollNumberValid) {
      setError("કૃપા કરીને 1 થી 8 ધોરણ અને માન્ય રોલ નંબર લખો.");
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const data = await apiRequest<ExamResultResponse>(
        `/api/v1/public/exam-results?standard=${encodeURIComponent(standard)}&rollNumber=${encodeURIComponent(rollNumber)}&resultType=${resultType}`,
      );
      setResult(data);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setError("આ ધોરણ અને રોલ નંબર માટે કોઈ પરિણામ મળ્યું નથી.");
      } else {
        setError("પરિણામ લાવવામાં ભૂલ થઈ. કૃપા કરીને ફરી પ્રયાસ કરો.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="પરીક્ષા પરિણામ"
        description="ધોરણ અને રોલ નંબર દ્વારા વાર્ષિક પરીક્ષાનું પરિણામ જુઓ"
      />

      {!result && (
        <Card className="mx-auto max-w-lg p-6">
          <form onSubmit={handleSearch} className="space-y-4">
            <div>
              <h2 className="text-lg font-bold text-slate-900 mb-4">પરિણામ શોધો</h2>
              <div className="space-y-4">
                <label className="flex flex-col gap-2 text-base font-semibold text-slate-800">
                  પરિણામનો પ્રકાર
                  <select
                    value={resultType}
                    onChange={(event) =>
                      setResultType(event.target.value as "ANNUAL" | "EKAM_KASOTI")
                    }
                    className="min-h-14 w-full rounded-xl border border-slate-300 bg-white px-4 text-base font-medium text-slate-900 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100"
                  >
                    <option value="ANNUAL">પરીક્ષા પરિણામ</option>
                    <option value="EKAM_KASOTI">એકમ કસોટી પરિણામ</option>
                  </select>
                </label>
                <div className="grid gap-4 sm:grid-cols-2">
                  <Input
                    label="ધોરણ"
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    value={standard}
                    onChange={(e) => setStandard(e.target.value.replace(/\D/g, "").slice(0, 1))}
                    placeholder="દા.ત. 8"
                    aria-invalid={standard.length > 0 && !isStandardValid}
                    required
                  />
                  <Input
                    label="રોલ નંબર"
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    value={rollNumber}
                    onChange={(e) => setRollNumber(e.target.value.replace(/\D/g, ""))}
                    placeholder="દા.ત. 1"
                    aria-invalid={rollNumber.length > 0 && !isRollNumberValid}
                    required
                  />
                </div>
              </div>
            </div>

            {error && (
              <p className="text-sm font-medium text-red-600 bg-red-50 p-3 rounded-lg">{error}</p>
            )}

            <Button
              type="submit"
              variant="primary"
              className="w-full gap-2 mt-2"
              loading={loading}
              loadingText="પરિણામ શોધી રહ્યા છીએ..."
              disabled={!isStandardValid || !isRollNumberValid}
            >
              <Search size={18} /> પરિણામ જુઓ
            </Button>
          </form>
        </Card>
      )}

      {result && (
        <div className="space-y-4">
          <div className="flex justify-between items-center bg-blue-50 p-3 rounded-lg border border-blue-100 print:hidden">
            <span className="font-semibold text-blue-900">
              ધોરણ: {standard} | રોલ નંબર: {rollNumber}
            </span>
            <Button variant="outline" size="sm" onClick={() => setResult(null)}>
              બીજું પરિણામ શોધો
            </Button>
          </div>

          <ExamResultViewer result={result} schoolName={school.name} logoUrl={school.logoUrl} />
        </div>
      )}
    </div>
  );
}
