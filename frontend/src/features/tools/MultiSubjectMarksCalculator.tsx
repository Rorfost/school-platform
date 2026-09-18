import { useState } from "react";
import { ListPlus, Plus, RotateCcw, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ToolCard, ToolResult } from "@/features/tools/ToolCard";
import { calculateMultiSubjectMarks, formatPercentage, type MultiSubjectMarksResult } from "@/features/tools/utils/school-calculations";

interface SubjectRow {
  id: number;
  name: string;
  obtained: string;
  maximum: string;
}

const blankSubject = (id: number): SubjectRow => ({ id, name: "", obtained: "", maximum: "" });

export function MultiSubjectMarksCalculator() {
  const [subjects, setSubjects] = useState<SubjectRow[]>([blankSubject(1)]);
  const [nextId, setNextId] = useState(2);
  const [result, setResult] = useState<MultiSubjectMarksResult | null>(null);
  const [error, setError] = useState("");

  function updateSubject(id: number, field: keyof Omit<SubjectRow, "id">, value: string) {
    setSubjects((current) => current.map((subject) => subject.id === id ? { ...subject, [field]: value } : subject));
    setResult(null);
    setError("");
  }

  function addSubject() {
    setSubjects((current) => [...current, blankSubject(nextId)]);
    setNextId((current) => current + 1);
  }

  function calculate() {
    const filledSubjects = subjects.filter((subject) => subject.obtained !== "" || subject.maximum !== "");
    if (filledSubjects.length === 0) {
      setError("ઓછામાં ઓછા એક વિષયના ગુણ દાખલ કરો.");
      return;
    }
    if (filledSubjects.some((subject) => subject.obtained === "" || subject.maximum === "")) {
      setError("દરેક વિષય માટે મેળવેલા અને કુલ ગુણ દાખલ કરો.");
      return;
    }
    const calculated = calculateMultiSubjectMarks(filledSubjects.map((subject) => ({ obtained: Number(subject.obtained), maximum: Number(subject.maximum) })));
    if (!calculated) {
      setError("મેળવેલા ગુણ 0 થી કુલ ગુણ સુધી અને કુલ ગુણ 0 કરતાં વધારે હોવા જોઈએ.");
      return;
    }
    setError("");
    setResult(calculated);
  }

  function clearAll() {
    setSubjects([blankSubject(nextId)]);
    setNextId((current) => current + 1);
    setResult(null);
    setError("");
  }

  return (
    <ToolCard title="એકથી વધુ વિષયના ગુણ" description="બધા વિષયના કુલ ગુણ અને સરેરાશ ટકાવારી જાણો." icon={ListPlus}>
      <div className="space-y-3">
        {subjects.map((subject, index) => (
          <fieldset key={subject.id} className="rounded-lg border border-slate-200 p-3">
            <legend className="px-1 text-sm font-semibold text-slate-700">વિષય {index + 1}</legend>
            <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)_auto] sm:items-end">
              <Input label="વિષયનું નામ (વૈકલ્પિક)" value={subject.name} onChange={(event) => updateSubject(subject.id, "name", event.target.value)} />
              <Input type="number" inputMode="decimal" min="0" step="any" label="મેળવેલા ગુણ" value={subject.obtained} onChange={(event) => updateSubject(subject.id, "obtained", event.target.value)} />
              <Input type="number" inputMode="decimal" min="0.01" step="any" label="કુલ ગુણ" value={subject.maximum} onChange={(event) => updateSubject(subject.id, "maximum", event.target.value)} />
              <Button
                type="button"
                variant="outline"
                aria-label={`વિષય ${index + 1} દૂર કરો`}
                onClick={() => {
                  setSubjects((current) =>
                    current.length > 1 ? current.filter((item) => item.id !== subject.id) : current,
                  );
                  setResult(null);
                  setError("");
                }}
                disabled={subjects.length === 1}
              >
                <Trash2 size={16} aria-hidden="true" /> દૂર કરો
              </Button>
            </div>
          </fieldset>
        ))}
      </div>
      {error && <p className="text-sm font-medium text-red-600" role="alert">{error}</p>}
      <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap">
        <Button type="button" variant="outline" className="w-full sm:w-auto" onClick={addSubject}><Plus size={16} aria-hidden="true" /> વિષય ઉમેરો</Button>
        <Button type="button" className="w-full sm:w-auto" onClick={calculate}>ગણતરી કરો</Button>
        <Button type="button" variant="outline" className="w-full sm:w-auto" onClick={clearAll}><RotateCcw size={16} aria-hidden="true" /> બધું સાફ કરો</Button>
      </div>
      {result && (
        <ToolResult>
          <dl className="grid grid-cols-2 gap-3 text-center sm:grid-cols-4">
            <div><dt className="text-xs text-slate-600">કુલ મેળવેલા</dt><dd className="mt-1 font-bold">{result.totalObtained}</dd></div>
            <div><dt className="text-xs text-slate-600">કુલ ગુણ</dt><dd className="mt-1 font-bold">{result.totalMaximum}</dd></div>
            <div><dt className="text-xs text-slate-600">કુલ ટકાવારી</dt><dd className="mt-1 font-bold">{formatPercentage(result.overallPercentage)}</dd></div>
            <div><dt className="text-xs text-slate-600">સરેરાશ ટકાવારી</dt><dd className="mt-1 font-bold">{formatPercentage(result.averagePercentage)}</dd></div>
          </dl>
        </ToolResult>
      )}
    </ToolCard>
  );
}
