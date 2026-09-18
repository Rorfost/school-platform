import { useState } from "react";
import { Languages, RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ToolCard, ToolResult } from "@/features/tools/ToolCard";
import { numberToEnglishWords } from "@/features/tools/utils/number-to-words";

export function NumberToWords() {
  const [number, setNumber] = useState("");
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState("");

  function calculate() {
    if (number === "") {
      setError("નંબર દાખલ કરો.");
      setResult(null);
      return;
    }
    const words = numberToEnglishWords(Number(number));
    if (!words) {
      setError("0 થી 999999 સુધીનો પૂર્ણાંક નંબર દાખલ કરો.");
      setResult(null);
      return;
    }
    setError("");
    setResult(words);
  }

  return (
    <ToolCard
      title="નંબરને શબ્દોમાં"
      description="0 થી 999999 સુધીના નંબરને અંગ્રેજી શબ્દોમાં લખો."
      icon={Languages}
    >
      <Input
        type="number"
        inputMode="numeric"
        min="0"
        max="999999"
        step="1"
        label="નંબર"
        helperText="ભાષા: અંગ્રેજી"
        value={number}
        onChange={(event) => {
          setNumber(event.target.value);
          setResult(null);
          setError("");
        }}
      />
      {error && (
        <p className="text-sm font-medium text-red-600" role="alert">
          {error}
        </p>
      )}
      <div className="flex flex-col gap-2 sm:flex-row">
        <Button type="button" className="w-full sm:w-auto" onClick={calculate}>
          શબ્દોમાં લખો
        </Button>
        <Button
          type="button"
          variant="outline"
          className="w-full sm:w-auto"
          onClick={() => {
            setNumber("");
            setResult(null);
            setError("");
          }}
        >
          <RotateCcw size={16} aria-hidden="true" /> ફરીથી શરૂ કરો
        </Button>
      </div>
      {result && (
        <ToolResult>
          <p className="text-lg font-bold capitalize">{result}</p>
        </ToolResult>
      )}
    </ToolCard>
  );
}
