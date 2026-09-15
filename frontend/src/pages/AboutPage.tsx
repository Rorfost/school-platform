import { Award, BookOpen, Calendar } from "lucide-react";
import schoolLogo from "@/assets/school-logo.jpeg";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";
import { LABELS, toGujaratiNumber } from "@/utils/gujarati";

export function AboutPage() {
  const school = useEffectiveSchoolInfo();

  return (
    <div className="space-y-8">
      <PageHeader
        title={LABELS.about}
        description="શાળાનો પરિચય, ઇતિહાસ અને શૈક્ષણિક ઉદ્દેશ્યો"
        backTo="/"
        backLabel={LABELS.home}
      />

      {/* Main Profile */}
      <Card className="p-6 sm:p-8">
        <div className="flex flex-col md:flex-row items-center md:items-start gap-6 sm:gap-8">
          <img
            src={schoolLogo}
            alt="શાળા લોગો"
            className="size-28 sm:size-36 rounded-full object-contain border border-blue-100 shadow-sm p-1"
          />
          <div className="flex-1 text-center md:text-left">
            <h2 className="text-xl sm:text-2xl font-bold text-slate-900">
              {school.name}
            </h2>
            <p className="mt-1 text-sm sm:text-base text-slate-600">
              {school.address}
            </p>
            <p className="mt-2 text-xs sm:text-sm font-semibold text-blue-900">
              ॥ સા વિદ્યા યા વિમુક્તયે ॥
            </p>
            <p className="mt-4 text-sm sm:text-base leading-relaxed text-slate-700">
              અમારી શાળા સમી તાલુકાના ધધાણા ગામમાં સ્થિત એક અગ્રણી પ્રાથમિક શાળા છે. વર્ષ {toGujaratiNumber(school.establishedYear)} માં સ્થાપના પામેલી આ શાળામાં ધોરણ ૧ થી ૮ સુધીના બાળકોને સંસ્કારયુક્ત અને ગુણવત્તાયુક્ત પ્રાથમિક શિક્ષણ આપવામાં આવે છે.
            </p>
          </div>
        </div>
      </Card>

      {/* Features Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-5">
        <Card>
          <div className="flex size-10 items-center justify-center rounded-lg bg-blue-50 text-blue-900 mb-3">
            <Calendar size={20} aria-hidden="true" />
          </div>
          <h3 className="text-base font-semibold text-slate-900">સ્થાપના વર્ષ</h3>
          <p className="mt-1 text-sm text-slate-600">
            ઇ.સ. {toGujaratiNumber(school.establishedYear)} થી શિક્ષણ ક્ષેત્રે અવિરત સેવા.
          </p>
        </Card>

        <Card>
          <div className="flex size-10 items-center justify-center rounded-lg bg-blue-50 text-blue-900 mb-3">
            <BookOpen size={20} aria-hidden="true" />
          </div>
          <h3 className="text-base font-semibold text-slate-900">શિક્ષણનું માધ્યમ</h3>
          <p className="mt-1 text-sm text-slate-600">
            ગુજરાતી માધ્યમ — ધોરણ ૧ થી ૮ સુધીનું પ્રાથમિક શિક્ષણ.
          </p>
        </Card>

        <Card>
          <div className="flex size-10 items-center justify-center rounded-lg bg-blue-50 text-blue-900 mb-3">
            <Award size={20} aria-hidden="true" />
          </div>
          <h3 className="text-base font-semibold text-slate-900">પીએમ શ્રી શાળા</h3>
          <p className="mt-1 text-sm text-slate-600">
            આધુનિક શૈક્ષણિક સુવિધાઓ અને સર્વાંગી બાળ વિકાસ માટે પ્રતિબદ્ધ.
          </p>
        </Card>
      </div>
    </div>
  );
}
