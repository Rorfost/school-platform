import { useState } from "react";
import { Percent, RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ToolCard, ToolResult } from "@/features/tools/ToolCard";
import {
  calculateMarks,
  formatPercentage,
  type MarksResult,
} from "@/features/tools/utils/school-calculations";

export function MarksCalculator() {
  const [obtainedMarks, setObtainedMarks] = useState("");
  const [maximumMarks, setMaximumMarks] = useState("");
  const [result, setResult] = useState<MarksResult | null>(null);
  const [error, setError] = useState("");

  function clearResult() {
    setResult(null);
    setError("");
  }

  function calculate() {
    if (obtainedMarks === "" || maximumMarks === "") {
      setError("મેળવેલા અને કુલ ગુણ દાખલ કરો.");
      return;
    }
    const calculated = calculateMarks(Number(obtainedMarks), Number(maximumMarks));
    if (!calculated) {
      setError("કુલ ગુણ 0 કરતાં વધારે અને મેળવેલા ગુણ 0 થી કુલ ગુણ સુધી હોવા જોઈએ.");
      return;
    }
    setError("");
    setResult(calculated);
  }

  return (
    <ToolCard
      title="ગુણ ટકાવારી"
      description="મેળવેલા અને કુલ ગુણ પરથી ટકાવારી જાણો."
      icon={Percent}
    >
      <div className="grid gap-3 sm:grid-cols-2">
        <Input
          type="number"
          inputMode="decimal"
          min="0"
          step="any"
          label="મેળવેલા ગુણ"
          value={obtainedMarks}
          onChange={(event) => {
            setObtainedMarks(event.target.value);
            clearResult();
          }}
        />
        <Input
          type="number"
          inputMode="decimal"
          min="0.01"
          step="any"
          label="કુલ ગુણ"
          value={maximumMarks}
          onChange={(event) => {
            setMaximumMarks(event.target.value);
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
            setObtainedMarks("");
            setMaximumMarks("");
            clearResult();
          }}
        >
          <RotateCcw size={16} aria-hidden="true" /> ફરીથી શરૂ કરો
        </Button>
      </div>
      {result && (
        <ToolResult>
          <p className="text-lg font-bold">ટકાવારી: {formatPercentage(result.percentage)}</p>
        </ToolResult>
      )}
    </ToolCard>
  );
}
