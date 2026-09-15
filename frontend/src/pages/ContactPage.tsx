import { useState } from "react";
import { Building2, Calendar, CheckCircle2, Mail, MapPin, Send, Shield } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";
import { LABELS, toGujaratiNumber } from "@/utils/gujarati";

export function ContactPage() {
  const school = useEffectiveSchoolInfo();
  const [fullName, setFullName] = useState("");
  const [phone, setPhone] = useState("");
  const [subject, setSubject] = useState("");
  const [message, setMessage] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName || !message) return;
    setIsSubmitting(true);
    setTimeout(() => {
      setIsSubmitting(false);
      setSubmitted(true);
    }, 600);
  };

  return (
    <div className="space-y-8 max-w-5xl">
      <PageHeader
        title={LABELS.contact}
        description="શાળા કાર્યાલય અને આચાર્યશ્રીનો સંપર્ક કરવા માટેની વિગતો"
        backTo="/"
        backLabel={LABELS.home}
      />

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="space-y-6">
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
                <p className="mt-0.5 text-slate-600">
                  ગામ: ધધાણા, તાલુકો: સમી, જિલ્લો: પાટણ - ૩૮૪૨૪૫
                </p>
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
                <p className="mt-0.5 text-slate-600">{toGujaratiNumber(school.establishedYear)}</p>
              </div>
            </div>
          </Card>
        </div>

        {/* Contact Form */}
        <Card className="p-6 sm:p-8">
          <h2 className="text-lg font-bold text-slate-900 mb-1">સંપર્ક / પ્રશ્ન મોકલો</h2>
          <p className="text-xs sm:text-sm text-slate-600 mb-5">
            શાળા સંબંધિત કોઈપણ પૂછપરછ માટે નીચેનું ફોર્મ ભરો.
          </p>

          {submitted ? (
            <div className="p-6 text-center space-y-3 bg-emerald-50 rounded-xl border border-emerald-200">
              <CheckCircle2 size={40} className="mx-auto text-emerald-600" />
              <h3 className="text-base font-bold text-emerald-900">
                આપનો સંદેશ સફળતાપૂર્વક મળ્યો છે!
              </h3>
              <p className="text-xs sm:text-sm text-emerald-700">
                અમે ટૂંક સમયમાં આપનો સંપર્ક કરીશું. આભાર!
              </p>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setSubmitted(false);
                  setFullName("");
                  setPhone("");
                  setSubject("");
                  setMessage("");
                }}
              >
                બીજો સંદેશ મોકલો
              </Button>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              <Input
                label="આપનું નામ"
                required
                placeholder="દા.ત. પટેલ રમેશભાઈ"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
              />
              <Input
                label="મોબાઈલ નંબર (મરજિયાત)"
                type="tel"
                placeholder="૧૦ અંકનો મોબાઈલ નંબર"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
              <Input
                label="વિષય"
                placeholder="દા.ત. પ્રવેશ અંગેની માહિતી"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
              />
              <div>
                <label className="block text-xs sm:text-sm font-medium text-slate-700 mb-1">
                  સંદેશ / વિગત <span className="text-red-500">*</span>
                </label>
                <textarea
                  required
                  rows={4}
                  className="w-full rounded-lg border border-slate-300 p-2.5 text-sm text-slate-900 focus:border-blue-900 focus:outline-none focus:ring-1 focus:ring-blue-900"
                  placeholder="આપનો પ્રશ્ન અથવા સંદેશ અહીં લખો..."
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                />
              </div>
              <Button
                type="submit"
                variant="primary"
                className="w-full gap-2"
                loading={isSubmitting}
              >
                <Send size={16} aria-hidden="true" />
                <span>સંદેશ મોકલો</span>
              </Button>
            </form>
          )}
        </Card>
      </div>
    </div>
  );
}
