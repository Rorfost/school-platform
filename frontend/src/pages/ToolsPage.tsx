import { useMemo, useState } from "react";
import { Calculator, RotateCcw } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";

const today = new Date().toISOString().slice(0, 10);

function difference(start: string, end: string) {
  const from = new Date(`${start}T00:00:00`);
  const to = new Date(`${end}T00:00:00`);
  if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime()) || from > to) return null;
  let years = to.getFullYear() - from.getFullYear();
  let months = to.getMonth() - from.getMonth();
  let days = to.getDate() - from.getDate();
  if (days < 0) {
    const previousMonthDays = new Date(to.getFullYear(), to.getMonth(), 0).getDate();
    days += previousMonthDays;
    months -= 1;
  }
  if (months < 0) {
    months += 12;
    years -= 1;
  }
  return {
    years,
    months,
    days,
    totalDays: Math.floor((to.getTime() - from.getTime()) / 86_400_000),
  };
}

export function ToolsPage() {
  const [dob, setDob] = useState("");
  const [asOn, setAsOn] = useState(today);
  const [start, setStart] = useState("");
  const [end, setEnd] = useState(today);
  const [obtained, setObtained] = useState("");
  const [total, setTotal] = useState("");
  const [present, setPresent] = useState("");
  const [working, setWorking] = useState("");
  const age = useMemo(() => (dob ? difference(dob, asOn) : null), [asOn, dob]);
  const dateDifference = useMemo(() => (start ? difference(start, end) : null), [end, start]);
  const percentage =
    Number(obtained) >= 0 && Number(total) > 0 ? (Number(obtained) / Number(total)) * 100 : null;
  const attendance =
    Number(working) > 0 && Number(present) >= 0 && Number(present) <= Number(working)
      ? (Number(present) / Number(working)) * 100
      : null;

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <PageHeader
        title="શિક્ષક સાધનો"
        description="ઝડપી, ખાનગી ગણતરીઓ — કોઈ માહિતી સંગ્રહિત થતી નથી."
        backTo="/"
        backLabel="હોમ"
      />
      <Card className="space-y-4">
        <div className="flex items-center gap-2">
          <Calculator className="text-blue-900" aria-hidden="true" />
          <h2 className="text-lg font-bold">ઉંમર ગણક</h2>
        </div>
        <div className="grid gap-3 sm:grid-cols-2">
          <DateField label="જન્મ તારીખ" value={dob} onChange={setDob} />
          <DateField label="કઈ તારીખે ઉંમર" value={asOn} onChange={setAsOn} />
        </div>
        {dob && (
          <Result
            text={
              age
                ? `${age.years} વર્ષ, ${age.months} મહિના, ${age.days} દિવસ`
                : "જન્મ તારીખ કઈ તારીખે ઉંમર કરતાં પછીની ન હોવી જોઈએ."
            }
          />
        )}
      </Card>
      <Card className="space-y-4">
        <h2 className="text-lg font-bold">તારીખનો તફાવત</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          <DateField label="શરૂઆતની તારીખ" value={start} onChange={setStart} />
          <DateField label="અંતિમ તારીખ" value={end} onChange={setEnd} />
        </div>
        {start && (
          <Result
            text={
              dateDifference
                ? `${dateDifference.years} વર્ષ, ${dateDifference.months} મહિના, ${dateDifference.days} દિવસ · કુલ ${dateDifference.totalDays} દિવસ`
                : "અંતિમ તારીખ શરૂઆતની તારીખ પછીની હોવી જોઈએ."
            }
          />
        )}
      </Card>
      <div className="grid gap-6 md:grid-cols-2">
        <Card className="space-y-4">
          <h2 className="text-lg font-bold">ટકાવારી</h2>
          <NumberField label="મેળવેલા ગુણ" value={obtained} onChange={setObtained} />
          <NumberField label="કુલ ગુણ" value={total} onChange={setTotal} min="1" />
          {total && (
            <Result
              text={
                percentage === null
                  ? "કુલ ગુણ શૂન્ય કરતાં વધારે હોવા જોઈએ."
                  : `${percentage.toFixed(2)}%`
              }
            />
          )}
        </Card>
        <Card className="space-y-4">
          <h2 className="text-lg font-bold">હાજરી</h2>
          <NumberField label="હાજર દિવસ" value={present} onChange={setPresent} min="0" />
          <NumberField label="કામકાજના દિવસ" value={working} onChange={setWorking} min="1" />
          {working && (
            <Result
              text={
                attendance === null
                  ? "હાજર દિવસ કામકાજના દિવસ કરતાં વધારે ન હોઈ શકે."
                  : `${attendance.toFixed(2)}% હાજરી · ${Number(working) - Number(present)} ગેરહાજર દિવસ`
              }
            />
          )}
        </Card>
      </div>
      <Button
        variant="outline"
        className="w-full sm:w-auto"
        onClick={() => {
          setDob("");
          setAsOn(today);
          setStart("");
          setEnd(today);
          setObtained("");
          setTotal("");
          setPresent("");
          setWorking("");
        }}
      >
        <RotateCcw size={16} aria-hidden="true" /> ફરીથી શરૂ કરો
      </Button>
    </div>
  );
}

function DateField({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="text-sm font-medium text-slate-700">
      {label}
      <input
        type="date"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-1.5 min-h-11 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-900"
      />
    </label>
  );
}
function NumberField({
  label,
  value,
  onChange,
  min,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  min?: string;
}) {
  return (
    <label className="block text-sm font-medium text-slate-700">
      {label}
      <input
        type="number"
        min={min}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-1.5 min-h-11 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-900"
      />
    </label>
  );
}
function Result({ text }: { text: string }) {
  return (
    <div className="rounded-lg border border-blue-200 bg-blue-50 p-3 text-sm font-semibold text-blue-950">
      {text}
    </div>
  );
}
