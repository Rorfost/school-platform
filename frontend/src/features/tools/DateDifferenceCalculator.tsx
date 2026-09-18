import { useState } from "react";
import { ArrowLeftRight, CalendarRange, RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ToolCard, ToolResult } from "@/features/tools/ToolCard";
import {
  calculateCalendarDifference,
  compareCalendarDates,
  getDayOfWeek,
  parseCalendarDate,
  todayCalendarDate,
  type CalendarDifference,
} from "@/features/tools/utils/date-calculations";

export function DateDifferenceCalculator() {
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState(todayCalendarDate);
  const [result, setResult] = useState<CalendarDifference | null>(null);
  const [error, setError] = useState("");

  function clearResult() {
    setResult(null);
    setError("");
  }

  function calculate() {
    const start = parseCalendarDate(startDate);
    const end = parseCalendarDate(endDate);
    if (!start || !end) {
      setError("શરૂઆત અને અંતિમ તારીખ પસંદ કરો.");
      return;
    }
    if (compareCalendarDates(start, end) > 0) {
      setError("અંતિમ તારીખ શરૂઆતની તારીખ કરતાં પહેલાંની હોઈ શકતી નથી.");
      return;
    }
    setError("");
    setResult(calculateCalendarDifference(startDate, endDate));
  }

  return (
    <ToolCard
      title="તારીખ વચ્ચેનો તફાવત"
      description="રેકોર્ડ, પ્રમાણપત્ર અને સમયગાળા માટે તારીખોનો તફાવત જાણો."
      icon={CalendarRange}
    >
      <div className="grid gap-3 sm:grid-cols-2">
        <Input
          type="date"
          label="શરૂઆતની તારીખ"
          value={startDate}
          onChange={(event) => {
            setStartDate(event.target.value);
            clearResult();
          }}
        />
        <Input
          type="date"
          label="અંતિમ તારીખ"
          value={endDate}
          onChange={(event) => {
            setEndDate(event.target.value);
            clearResult();
          }}
        />
      </div>
      {error && (
        <p className="text-sm font-medium text-red-600" role="alert">
          {error}
        </p>
      )}
      <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap">
        <Button type="button" className="w-full sm:w-auto" onClick={calculate}>
          ગણતરી કરો
        </Button>
        <Button
          type="button"
          variant="outline"
          className="w-full sm:w-auto"
          onClick={() => {
            setStartDate(endDate);
            setEndDate(startDate);
            clearResult();
          }}
          disabled={!startDate || !endDate}
        >
          <ArrowLeftRight size={16} aria-hidden="true" /> તારીખ બદલો
        </Button>
        <Button
          type="button"
          variant="outline"
          className="w-full sm:w-auto"
          onClick={() => {
            setStartDate("");
            setEndDate(todayCalendarDate());
            clearResult();
          }}
        >
          <RotateCcw size={16} aria-hidden="true" /> ફરીથી શરૂ કરો
        </Button>
      </div>
      {result && (
        <ToolResult>
          <p className="font-bold">
            તફાવત: {result.years} વર્ષ, {result.months} મહિના, {result.days} દિવસ
          </p>
          <p className="mt-1">
            કુલ દિવસ: <strong>{result.totalDays}</strong>
          </p>
          <p className="mt-1 text-xs text-slate-600">
            શરૂઆત: {getDayOfWeek(startDate)} · અંતિમ: {getDayOfWeek(endDate)}
          </p>
        </ToolResult>
      )}
    </ToolCard>
  );
}
