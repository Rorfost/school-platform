import { FileWarning } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";

export function ResultsInfoPage() {
  return (
    <div className="space-y-6">
      <PageHeader title="પરિણામ" description="વિદ્યાર્થીનું પરિણામ" />
      <Card className="mx-auto max-w-2xl">
        <div className="flex gap-4">
          <FileWarning className="mt-0.5 shrink-0 text-amber-700" size={26} aria-hidden="true" />
          <div>
            <h2 className="text-lg font-bold text-slate-900">પરિણામ સેવા ટૂંક સમયમાં ઉપલબ્ધ થશે</h2>
            <p className="mt-2 text-sm leading-relaxed text-slate-700">
              શાળા સુરક્ષિત રોલ નંબર અને PIN પ્રક્રિયા પૂર્ણ કર્યા પછી વ્યક્તિગત પરિણામ અહીં જોઈ
              શકાશે.
            </p>
            <p className="mt-3 text-sm leading-relaxed text-slate-600">
              હાલમાં કોઈ વિદ્યાર્થીનું પરિણામ ઓનલાઈન ઉપલબ્ધ નથી. વધુ માહિતી માટે શાળાનો સંપર્ક કરો.
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}
