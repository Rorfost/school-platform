import { useState } from "react";
import { Grid2X2, RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ToolCard, ToolResult } from "@/features/tools/ToolCard";
import {
  generateMultiplicationTable,
  type MultiplicationRow,
} from "@/features/tools/utils/multiplication-table";

export function MultiplicationTable() {
  const [number, setNumber] = useState("");
  const [from, setFrom] = useState("1");
  const [to, setTo] = useState("10");
  const [rows, setRows] = useState<MultiplicationRow[]>([]);
  const [error, setError] = useState("");

  function clearResult() {
    setRows([]);
    setError("");
  }

  function calculate() {
    const tableNumber = Number(number);
    const start = Number(from);
    const end = Number(to);
    const table = generateMultiplicationTable(tableNumber, start, end);
    if (!table) {
      setError(
        Number.isInteger(tableNumber) && tableNumber > 0
          ? "શરૂઆત 1 થી અને અંત 20 સુધી રાખો; શરૂઆત અંત કરતાં મોટી ન હોવી જોઈએ."
          : "1 અથવા તેથી મોટો પૂર્ણાંક નંબર દાખલ કરો.",
      );
      return;
    }
    setError("");
    setRows(table);
  }

  return (
    <ToolCard title="પાડા" description="બાળકો માટે 1 થી 20 સુધીના પાડા બનાવો." icon={Grid2X2}>
      <div className="grid gap-3 sm:grid-cols-3">
        <Input
          type="number"
          inputMode="numeric"
          min="1"
          step="1"
          label="નંબર"
          value={number}
          onChange={(event) => {
            setNumber(event.target.value);
            clearResult();
          }}
        />
        <Input
          type="number"
          inputMode="numeric"
          min="1"
          max="20"
          step="1"
          label="થી"
          value={from}
          onChange={(event) => {
            setFrom(event.target.value);
            clearResult();
          }}
        />
        <Input
          type="number"
          inputMode="numeric"
          min="1"
          max="20"
          step="1"
          label="સુધી"
          value={to}
          onChange={(event) => {
            setTo(event.target.value);
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
          પાડો બનાવો
        </Button>
        <Button
          type="button"
          variant="outline"
          className="w-full sm:w-auto"
          onClick={() => {
            setNumber("");
            setFrom("1");
            setTo("10");
            clearResult();
          }}
        >
          <RotateCcw size={16} aria-hidden="true" /> ફરીથી શરૂ કરો
        </Button>
      </div>
      {rows.length > 0 && (
        <ToolResult>
          <ol className="grid gap-1.5 font-semibold sm:grid-cols-2" aria-label="ગુણાકારનો પાડો">
            {rows.map((row) => (
              <li key={row.multiplier}>
                {number} × {row.multiplier} = {row.product}
              </li>
            ))}
          </ol>
        </ToolResult>
      )}
    </ToolCard>
  );
}
