import { useState } from "react";
import { CalendarDays, Copy, RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ToolCard, ToolResult } from "@/features/tools/ToolCard";
import {
  calculateCalendarDifference,
  compareCalendarDates,
  formatCalendarDate,
  getDayOfWeek,
  parseCalendarDate,
  todayCalendarDate,
  type CalendarDifference,
} from "@/features/tools/utils/date-calculations";

function cutoffDate(month: number, day: number): string {
  const now = new Date();
  return formatCalendarDate({ year: now.getFullYear(), month, day });
}

export function AgeCalculator() {
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [asOnDate, setAsOnDate] = useState(todayCalendarDate);
  const [result, setResult] = useState<CalendarDifference | null>(null);
  const [error, setError] = useState("");
  const [copyMessage, setCopyMessage] = useState("");

  function clearResult() {
    setResult(null);
    setError("");
    setCopyMessage("");
  }

  function calculate() {
    const birthDate = parseCalendarDate(dateOfBirth);
    const asOn = parseCalendarDate(asOnDate);
    if (!birthDate) {
      setError("જન્મ તારીખ પસંદ કરો.");
      setResult(null);
      return;
    }
    if (!asOn) {
      setError("ઉંમર માટેની તારીખ પસંદ કરો.");
      setResult(null);
      return;
    }
    if (compareCalendarDates(birthDate, asOn) > 0) {
      setError("જન્મ તારીખ પસંદ કરેલી તારીખ પછીની હોઈ શકતી નથી.");
      setResult(null);
      return;
    }

    setError("");
    setResult(calculateCalendarDifference(dateOfBirth, asOnDate));
  }

  const resultText = result
    ? `ઉંમર: ${result.years} વર્ષ, ${result.months} મહિના, ${result.days} દિવસ. કુલ મહિના: ${result.totalMonths}. કુલ દિવસ: ${result.totalDays}.`
    : "";

  async function copyResult() {
    if (!navigator.clipboard) {
      setCopyMessage("પરિણામ કૉપી થઈ શક્યું નથી.");
      return;
    }
    try {
      await navigator.clipboard.writeText(resultText);
      setCopyMessage("પરિણામ કૉપી થયું.");
    } catch {
      setCopyMessage("પરિણામ કૉપી થઈ શક્યું નથી.");
    }
  }

  return (
    <ToolCard title="ઉંમર ગણતરી" description="પ્રવેશ અને શાળાના રેકોર્ડ માટે ચોક્કસ ઉંમર જાણો." icon={CalendarDays}>
      <div className="grid gap-3 sm:grid-cols-2">
        <Input
          type="date"
          label="જન્મ તારીખ"
          value={dateOfBirth}
          onChange={(event) => {
            setDateOfBirth(event.target.value);
            clearResult();
          }}
          error={error && !dateOfBirth ? error : undefined}
        />
        <Input
          type="date"
          label="ઉંમર કઈ તારીખે"
          value={asOnDate}
          onChange={(event) => {
            setAsOnDate(event.target.value);
            clearResult();
          }}
        />
      </div>
      <div className="flex flex-wrap gap-2" aria-label="ઝડપી તારીખ પસંદગી">
        <Button type="button" size="sm" variant="outline" onClick={() => { setAsOnDate(todayCalendarDate()); clearResult(); }}>
          આજની તારીખ
        </Button>
        <Button type="button" size="sm" variant="outline" onClick={() => { setAsOnDate(cutoffDate(5, 31)); clearResult(); }}>
          31 મે
        </Button>
        <Button type="button" size="sm" variant="outline" onClick={() => { setAsOnDate(cutoffDate(6, 1)); clearResult(); }}>
          1 જૂન
        </Button>
      </div>
      {error && dateOfBirth && <p className="text-sm font-medium text-red-600" role="alert">{error}</p>}
      <div className="flex flex-col gap-2 sm:flex-row">
        <Button type="button" className="w-full sm:w-auto" onClick={calculate}>ગણતરી કરો</Button>
        <Button
          type="button"
          variant="outline"
          className="w-full sm:w-auto"
          onClick={() => {
            setDateOfBirth("");
            setAsOnDate(todayCalendarDate());
            clearResult();
          }}
        >
          <RotateCcw size={16} aria-hidden="true" /> ફરીથી શરૂ કરો
        </Button>
      </div>
      {result && (
        <ToolResult>
          <p className="font-bold">ઉંમર</p>
          <dl className="mt-3 grid grid-cols-3 gap-2 text-center">
            <div className="rounded-md bg-white p-2"><dt className="text-xs text-slate-600">વર્ષ</dt><dd className="mt-1 text-lg font-bold">{result.years}</dd></div>
            <div className="rounded-md bg-white p-2"><dt className="text-xs text-slate-600">મહિના</dt><dd className="mt-1 text-lg font-bold">{result.months}</dd></div>
            <div className="rounded-md bg-white p-2"><dt className="text-xs text-slate-600">દિવસ</dt><dd className="mt-1 text-lg font-bold">{result.days}</dd></div>
          </dl>
          <p className="mt-3">કુલ મહિના: <strong>{result.totalMonths}</strong> · કુલ દિવસ: <strong>{result.totalDays}</strong></p>
          <p className="mt-1 text-xs text-slate-600">પસંદ કરેલી તારીખનો દિવસ: {getDayOfWeek(asOnDate)}</p>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <Button type="button" size="sm" variant="outline" onClick={copyResult}><Copy size={15} aria-hidden="true" /> પરિણામ કૉપી કરો</Button>
            {copyMessage && <span className="text-xs font-medium text-slate-700" role="status">{copyMessage}</span>}
          </div>
        </ToolResult>
      )}
    </ToolCard>
  );
}
