import { Building2, Calendar, Mail, MapPin, Shield } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";
import { LABELS, toGujaratiNumber } from "@/utils/gujarati";

export function ContactPage() {
  const school = useEffectiveSchoolInfo();

  return (
    <div className="space-y-8 max-w-4xl">
      <PageHeader
        title={LABELS.contact}
        description="શાળા કાર્યાલય અને આચાર્યશ્રીનો સંપર્ક કરવા માટેની વિગતો"
        backTo="/"
        backLabel={LABELS.home}
      />

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card className="p-6 space-y-4">
          <h2 className="text-lg font-bold text-slate-900 border-b border-slate-100 pb-3">
            શાળા સરનામું
          </h2>
          <div className="flex items-start gap-3 text-sm text-slate-700">
            <Building2 className="text-blue-900 shrink-0 mt-0.5" size={20} aria-hidden="true" />
            <div>
              <p className="font-semibold text-slate-900">{school.name}</p>
              <p className="mt-1 text-slate-600">{school.address}</p>
            </div>
          </div>
          <div className="flex items-start gap-3 text-sm text-slate-700">
            <MapPin className="text-blue-900 shrink-0 mt-0.5" size={20} aria-hidden="true" />
            <div>
              <p className="font-semibold text-slate-900">સ્થાન</p>
              <p className="mt-0.5 text-slate-600">ગામ: ધધાણા, તાલુકો: સમી, જિલ્લો: પાટણ</p>
            </div>
          </div>
        </Card>

        <Card className="p-6 space-y-4">
          <h2 className="text-lg font-bold text-slate-900 border-b border-slate-100 pb-3">
            ઈમેઈલ અને સત્તાવાર વિગત
          </h2>
          <div className="flex items-start gap-3 text-sm text-slate-700">
            <Mail className="text-blue-900 shrink-0 mt-0.5" size={20} aria-hidden="true" />
            <div>
              <p className="font-semibold text-slate-900">સત્તાવાર ઈમેઈલ</p>
              <a
                href={`mailto:${school.email}`}
                className="text-blue-900 hover:underline mt-0.5 block break-all font-mono text-xs sm:text-sm"
              >
                {school.email}
              </a>
            </div>
          </div>
          <div className="flex items-start gap-3 text-sm text-slate-700">
            <Shield className="text-blue-900 shrink-0 mt-0.5" size={20} aria-hidden="true" />
            <div>
              <p className="font-semibold text-slate-900">{LABELS.diseLabel}</p>
              <p className="mt-0.5 text-slate-600 font-mono">
                {toGujaratiNumber(school.schoolCode)}
              </p>
            </div>
          </div>
          <div className="flex items-start gap-3 text-sm text-slate-700">
            <Calendar className="text-blue-900 shrink-0 mt-0.5" size={20} aria-hidden="true" />
            <div>
              <p className="font-semibold text-slate-900">{LABELS.estLabel}</p>
              <p className="mt-0.5 text-slate-600">
                {toGujaratiNumber(school.establishedYear)}
              </p>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
