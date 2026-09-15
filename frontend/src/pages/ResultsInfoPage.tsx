import { AlertCircle, CheckCircle2, Lock, ShieldCheck } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { LABELS } from "@/utils/gujarati";

export function ResultsInfoPage() {
  return (
    <div className="space-y-6 max-w-3xl">
      <PageHeader
        title={LABELS.results}
        description="પરીક્ષા પરિણામ અને ગુણાંકન અંગે અગત્યની માહિતી"
        backTo="/student"
        backLabel={LABELS.studentCorner}
      />

      {/* Privacy Notice Banner */}
      <div
        className="flex items-start gap-3.5 rounded-xl border border-blue-200 bg-blue-50/80 p-5 text-blue-950 shadow-xs"
        role="region"
        aria-label="ગોપનીયતા સુરક્ષા"
      >
        <ShieldCheck className="mt-0.5 shrink-0 text-blue-800" size={22} aria-hidden="true" />
        <div>
          <h2 className="text-base font-bold text-blue-900">
            વિદ્યાર્થી પરિણામ ગોપનીયતા
          </h2>
          <p className="mt-1 text-sm text-blue-950/90 leading-relaxed">
            વિદ્યાર્થીઓની સુરક્ષા અને ગોપનીયતાના નિયમો મુજબ, આ પોર્ટલ પર સમગ્ર વર્ગનું પરિણામ ક્યારેય જાહેરમાં મુકાશે નહીં. દરેક વિદ્યાર્થી માત્ર પોતાનું જ પરિણામ સુરક્ષિત રીતે જોઈ શકશે.
          </p>
        </div>
      </div>

      {/* Status Overview */}
      <Card className="p-6">
        <h3 className="text-lg font-bold text-slate-900 mb-3">
          પરિણામ ચકાસણી પદ્ધતિ
        </h3>
        <p className="text-sm text-slate-600 leading-relaxed">
          જ્યારે પણ શાળા દ્વારા સત્તાવાર પરિણામ જાહેર કરવામાં આવશે, ત્યારે વિદ્યાર્થીઓ નીચેની વિગતો દાખલ કરીને પોતાનું પરિણામ જોઈ શકશે:
        </p>

        <ul className="mt-4 space-y-2.5 text-sm text-slate-700">
          <li className="flex items-center gap-2.5">
            <CheckCircle2 size={16} className="text-emerald-700 shrink-0" aria-hidden="true" />
            <span>ચાલુ શૈક્ષણિક વર્ષ અને કસોટીનો પ્રકાર</span>
          </li>
          <li className="flex items-center gap-2.5">
            <CheckCircle2 size={16} className="text-emerald-700 shrink-0" aria-hidden="true" />
            <span>વિદ્યાર્થીનું ધોરણ (ધોરણ ૧ થી ૮)</span>
          </li>
          <li className="flex items-center gap-2.5">
            <CheckCircle2 size={16} className="text-emerald-700 shrink-0" aria-hidden="true" />
            <span>શાળા દ્વારા ફાળવવામાં આવેલ રોલ નંબર</span>
          </li>
          <li className="flex items-center gap-2.5">
            <CheckCircle2 size={16} className="text-emerald-700 shrink-0" aria-hidden="true" />
            <span>સુરક્ષિત પરિણામ પિન (Result PIN)</span>
          </li>
        </ul>

        <div className="mt-6 border-t border-slate-100 pt-5 flex items-start gap-2.5 text-xs sm:text-sm text-slate-500">
          <Lock size={16} className="text-slate-400 shrink-0 mt-0.5" aria-hidden="true" />
          <span>
            હાલમાં પરિણામ સિસ્ટમની તકનીકી ગોઠવણ પ્રગતિમાં છે. પરિણામ જાહેર થતાં જ શાળા તરફથી સૂચના આપવામાં આવશે.
          </span>
        </div>
      </Card>
    </div>
  );
}
