import { useState } from "react";
import { Search } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { apiRequest, ApiError } from "@/api/client";
import type { ExamResultResponse } from "@/api/types";
import { ExamResultViewer } from "@/features/public/ExamResultViewer";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";
import { usePublicStandards } from "@/features/public/usePublicAcademic";

export function ResultsInfoPage() {
  const [resultType, setResultType] = useState<"ANNUAL" | "EKAM_KASOTI">("ANNUAL");
  const resultTypeLabel = resultType === "ANNUAL" ? "પરીક્ષા" : "ત્રિમાસિક કસોટી";
  const [standard, setStandard] = useState("");
  const [studentClass, setStudentClass] = useState("");
  const [rollNumber, setRollNumber] = useState("");
  const [result, setResult] = useState<ExamResultResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const school = useEffectiveSchoolInfo();
  const { data: standards } = usePublicStandards();

  const matchedStandard = standards?.find((s) => {
    if (!standard) return false;
    const codeDigits = s.code.replace(/\D/g, "");
    const nameDigits = s.displayName.replace(/\D/g, "");
    return (
      s.code === standard ||
      codeDigits === standard ||
      s.displayName === standard ||
      nameDigits === standard
    );
  });
  const configuredClasses = matchedStandard?.classes ?? [];

  const isStandardValid = /^[1-8]$/.test(standard);
  // Class is only valid when a specific class has been selected from the configured dropdown
  const isClassValid = configuredClasses.length > 0 && configuredClasses.includes(studentClass);
  const isRollNumberValid = /^[1-9]\d*$/.test(rollNumber);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isStandardValid) {
      setError("કૃપા કરીને ૧ થી ૮ સુધીનું માન્ય ધોરણ પસંદ/દાખલ કરો.");
      return;
    }
    if (!isClassValid) {
      setError(`કૃપા કરીને વર્ગ પસંદ કરો (${configuredClasses.join(", ")}).`);
      return;
    }
    if (!isRollNumberValid) {
      setError("કૃપા કરીને માન્ય રોલ નંબર દાખલ કરો.");
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const data = await apiRequest<ExamResultResponse>(
        `/api/v1/public/exam-results?standard=${encodeURIComponent(
          standard,
        )}&class=${encodeURIComponent(studentClass.trim())}&rollNumber=${encodeURIComponent(
          rollNumber,
        )}&resultType=${resultType}`,
      );
      setResult(data);
    } catch (err) {
      if (err instanceof ApiError && err.status === 0) {
        setError("નેટવર્ક કનેક્શન તપાસો અને ફરી પ્રયત્ન કરો.");
      } else {
        setError(
          `ધોરણ ${standard}, વર્ગ ${studentClass}, રોલ નંબર ${rollNumber} માટે ${resultTypeLabel} પરિણામ મળ્યું નથી.`,
        );
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="પરીક્ષા પરિણામ"
        description={`ધોરણ, વર્ગ અને રોલ નંબર દ્વારા ${resultTypeLabel} પરિણામ જુઓ`}
      />

      {!result && (
        <Card className="mx-auto max-w-lg p-5 sm:p-6">
          <form onSubmit={handleSearch} className="space-y-5">
            <h2 className="text-lg font-bold text-slate-900">પરિણામ શોધો</h2>

            {/* Result type */}
            <Select
              id="result-type"
              label="પરિણામનો પ્રકાર"
              value={resultType}
              onChange={(event) => setResultType(event.target.value as "ANNUAL" | "EKAM_KASOTI")}
            >
              <option value="ANNUAL">પરીક્ષા પરિણામ</option>
              <option value="EKAM_KASOTI">ત્રિમાસિક કસોટી પરિણામ</option>
            </Select>

            {/* Standard + Class — side by side */}
            <div className="grid grid-cols-2 gap-3">
              <Input
                label="ધોરણ"
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                value={standard}
                onChange={(e) => {
                  const val = e.target.value.replace(/\D/g, "").slice(0, 1);
                  setStandard(val);
                  setStudentClass("");
                }}
                placeholder="દા.ત. 8"
                aria-invalid={standard.length > 0 && !isStandardValid}
                required
              />
              <Select
                id="class-select"
                label="વર્ગ"
                aria-label="વર્ગ"
                value={studentClass}
                onChange={(e) => setStudentClass(e.target.value)}
                disabled={!isStandardValid || configuredClasses.length === 0}
                required
              >
                {!isStandardValid ? (
                  <option value="">ધોરણ દાખલ કરો</option>
                ) : configuredClasses.length === 0 ? (
                  <option value="">વર્ગ ઉપલબ્ધ નથી</option>
                ) : (
                  <>
                    <option value="">વર્ગ પસંદ કરો</option>
                    {configuredClasses.map((cls) => (
                      <option key={cls} value={cls}>
                        {cls}
                      </option>
                    ))}
                  </>
                )}
              </Select>
            </div>

            {/* Roll number — full width */}
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

            {error && (
              <p className="text-sm font-medium text-red-600 bg-red-50 p-3 rounded-lg">{error}</p>
            )}

            <Button
              type="submit"
              variant="primary"
              className="w-full gap-2"
              loading={loading}
              loadingText="પરિણામ શોધી રહ્યા છીએ..."
              disabled={!isStandardValid || !isClassValid || !isRollNumberValid}
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
              ધોરણ: {result.standard} | વર્ગ: {result.studentClass || studentClass} | રોલ નંબર:{" "}
              {result.rollNumber}
            </span>
            <Button variant="outline" size="sm" onClick={() => setResult(null)}>
              બીજું પરિણામ શોધો
            </Button>
          </div>

          <ExamResultViewer
            result={result}
            schoolName={school.name}
            logoUrl={school.logoUrl}
            resultType={resultType}
          />
        </div>
      )}
    </div>
  );
}
