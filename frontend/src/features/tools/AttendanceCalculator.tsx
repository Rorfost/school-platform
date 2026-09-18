import { useState } from "react";
import { ClipboardCheck, Copy, RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ToolCard, ToolResult } from "@/features/tools/ToolCard";
import {
  calculateAttendance,
  formatPercentage,
  type AttendanceResult,
} from "@/features/tools/utils/school-calculations";

export function AttendanceCalculator() {
  const [workingDays, setWorkingDays] = useState("");
  const [presentDays, setPresentDays] = useState("");
  const [result, setResult] = useState<AttendanceResult | null>(null);
  const [error, setError] = useState("");
  const [copyMessage, setCopyMessage] = useState("");

  function clearResult() {
    setResult(null);
    setError("");
    setCopyMessage("");
  }

  function calculate() {
    if (workingDays === "" || presentDays === "") {
      setError("કામકાજના અને હાજર દિવસ દાખલ કરો.");
      return;
    }
    const calculated = calculateAttendance(Number(workingDays), Number(presentDays));
    if (!calculated) {
      setError("કામકાજના દિવસ 0 કરતાં વધારે અને હાજર દિવસ 0 થી કામકાજના દિવસ સુધી હોવા જોઈએ.");
      return;
    }
    setError("");
    setResult(calculated);
  }

  async function copyResult() {
    if (!result || !navigator.clipboard) {
      setCopyMessage("પરિણામ કૉપી થઈ શક્યું નથી.");
      return;
    }
    try {
      await navigator.clipboard.writeText(
        `ગેરહાજર દિવસ: ${result.absentDays}. હાજરી: ${formatPercentage(result.percentage)}.`,
      );
      setCopyMessage("પરિણામ કૉપી થયું.");
    } catch {
      setCopyMessage("પરિણામ કૉપી થઈ શક્યું નથી.");
    }
  }

  return (
    <ToolCard
      title="હાજરી ટકાવારી"
      description="કામકાજના દિવસ અને હાજરી પરથી ટકાવારી જાણો."
      icon={ClipboardCheck}
    >
      <div className="grid gap-3 sm:grid-cols-2">
        <Input
          type="number"
          inputMode="numeric"
          min="1"
          step="1"
          label="કામકાજના દિવસ"
          value={workingDays}
          onChange={(event) => {
            setWorkingDays(event.target.value);
            clearResult();
          }}
        />
        <Input
          type="number"
          inputMode="numeric"
          min="0"
          step="1"
          label="હાજર દિવસ"
          value={presentDays}
          onChange={(event) => {
            setPresentDays(event.target.value);
            clearResult();
          }}
        />
      </div>
      {error && (
        <p className="text-sm font-medium text-red-600" role="alert">
          {error}
        </p>
      )}
      <div className="flex flex-col gap-2 sm:flex-row">
        <Button type="button" className="w-full sm:w-auto" onClick={calculate}>
          ગણતરી કરો
        </Button>
        <Button
          type="button"
          variant="outline"
          className="w-full sm:w-auto"
          onClick={() => {
            setWorkingDays("");
            setPresentDays("");
            clearResult();
          }}
        >
          <RotateCcw size={16} aria-hidden="true" /> ફરીથી શરૂ કરો
        </Button>
      </div>
      {result && (
        <ToolResult>
          <p>
            ગેરહાજર દિવસ: <strong>{result.absentDays}</strong>
          </p>
          <p className="mt-1 text-lg font-bold">હાજરી: {formatPercentage(result.percentage)}</p>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <Button type="button" size="sm" variant="outline" onClick={copyResult}>
              <Copy size={15} aria-hidden="true" /> પરિણામ કૉપી કરો
            </Button>
            {copyMessage && (
              <span className="text-xs font-medium text-slate-700" role="status">
                {copyMessage}
              </span>
            )}
          </div>
        </ToolResult>
      )}
    </ToolCard>
  );
}
