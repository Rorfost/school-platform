import { Building2, Mail, MapPin, MessageCircle, Phone, UserCheck } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { useEffectiveSchoolInfo, usePublicPrincipalProfile } from "@/features/school/useSchoolData";
import { LABELS } from "@/utils/gujarati";

const whatsappNumber = "919714862818";

export function ContactPage() {
  const school = useEffectiveSchoolInfo();
  const { data: principal } = usePublicPrincipalProfile();
  const principalName = principal?.fullName || "Rakesh Patel";
  const phone = principal?.phone || school.phone;

  return (
    <div className="max-w-5xl space-y-8">
      <PageHeader title={LABELS.contact} description="શાળા કાર્યાલય અને આચાર્યશ્રીનો સંપર્ક કરવા માટેની વિગતો" backTo="/" backLabel={LABELS.home} />
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <Card className="space-y-4">
          <h2 className="border-b border-slate-100 pb-3 text-lg font-bold text-slate-900">આચાર્યશ્રીનો સંપર્ક</h2>
          <div className="flex items-start gap-3 text-sm text-slate-700">
            <UserCheck className="mt-0.5 shrink-0 text-blue-900" size={20} aria-hidden="true" />
            <div><p className="font-semibold text-slate-900">{principalName}</p><p className="mt-1 text-slate-600">આચાર્યશ્રી</p></div>
          </div>
          <div className="flex items-start gap-3 text-sm text-slate-700">
            <Phone className="mt-0.5 shrink-0 text-blue-900" size={20} aria-hidden="true" />
            <div><p className="font-semibold text-slate-900">મોબાઇલ નંબર</p><a className="mt-1 block font-medium text-blue-900 hover:underline" href={`tel:${phone.replace(/\s/g, "")}`}>{phone}</a></div>
          </div>
          <div className="grid grid-cols-1 gap-2 pt-2 sm:grid-cols-2">
            <a href={`tel:${phone.replace(/\s/g, "")}`} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-lg bg-blue-900 px-4 py-2 text-sm font-medium text-white hover:bg-blue-950"><Phone size={16} aria-hidden="true" /> કૉલ કરો</a>
            <a href={`https://wa.me/${whatsappNumber}`} target="_blank" rel="noreferrer" className="inline-flex min-h-11 items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-800 hover:bg-slate-50"><MessageCircle size={16} aria-hidden="true" /> WhatsApp</a>
          </div>
        </Card>
        <div className="space-y-6">
          <Card className="space-y-4">
            <h2 className="border-b border-slate-100 pb-3 text-lg font-bold text-slate-900">શાળાનું સરનામું</h2>
            <div className="flex items-start gap-3 text-sm text-slate-700"><Building2 className="mt-0.5 shrink-0 text-blue-900" size={20} aria-hidden="true" /><div><p className="font-semibold text-slate-900">{school.name}</p><p className="mt-1 text-slate-600">{school.address}</p></div></div>
            <div className="flex items-start gap-3 text-sm text-slate-700"><MapPin className="mt-0.5 shrink-0 text-blue-900" size={20} aria-hidden="true" /><p className="text-slate-600">{[school.city, school.state, school.postalCode].filter(Boolean).join(", ")}</p></div>
          </Card>
          {school.email && <Card className="flex items-start gap-3"><Mail className="mt-0.5 shrink-0 text-blue-900" size={20} aria-hidden="true" /><div className="min-w-0"><p className="font-semibold text-slate-900">ઇમેઇલ</p><a href={`mailto:${school.email}`} className="mt-1 block break-all text-sm text-blue-900 hover:underline">{school.email}</a></div></Card>}
        </div>
      </div>
    </div>
  );
}
