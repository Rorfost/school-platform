import { Lock, Mail, MapPin } from "lucide-react";
import { Link } from "react-router-dom";
import schoolLogo from "@/assets/school-logo.jpeg";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";
import { LABELS, toGujaratiNumber } from "@/utils/gujarati";

export function Footer() {
  const school = useEffectiveSchoolInfo();

  return (
    <footer className="border-t border-slate-200 bg-white text-slate-700">
      <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 sm:py-10">
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          {/* School Brand & Motto */}
          <div>
            <div className="flex items-center gap-3">
              <img
                src={school.logoUrl ?? schoolLogo}
                onError={(event) => {
                  event.currentTarget.onerror = null;
                  event.currentTarget.src = schoolLogo;
                }}
                alt="શાળા લોગો"
                className="size-11 rounded-full object-contain border border-blue-100"
              />
              <div>
                <h3 className="text-base font-bold text-slate-900 leading-tight">{school.name}</h3>
                <p className="text-xs text-slate-500 font-medium mt-0.5">
                  {LABELS.estLabel}: {toGujaratiNumber(school.establishedYear)}
                </p>
              </div>
            </div>
            <p className="mt-3 text-xs sm:text-sm text-slate-600 leading-relaxed">
              પ્રાથમિક શિક્ષણ દ્વારા બાળકોમાં સર્વાંગી વિકાસ અને સંસ્કાર સિંચન.
            </p>
            <div className="mt-3 text-xs font-semibold text-blue-900">
              ॥ સા વિદ્યા યા વિમુક્તયે ॥
            </div>
          </div>

          {/* Contact Details */}
          <div>
            <h4 className="text-sm font-semibold text-slate-900 mb-3">{LABELS.contact}</h4>
            <div className="space-y-2.5 text-xs sm:text-sm text-slate-600">
              <div className="flex items-start gap-2">
                <MapPin size={16} className="mt-0.5 shrink-0 text-blue-900" aria-hidden="true" />
                <span>{school.address}</span>
              </div>
              <div className="flex items-center gap-2">
                <Mail size={16} className="shrink-0 text-blue-900" aria-hidden="true" />
                <a
                  href={`mailto:${school.email}`}
                  className="hover:text-blue-900 transition-colors break-all"
                >
                  {school.email}
                </a>
              </div>
              <div className="pt-2 text-xs text-slate-500">
                {LABELS.diseLabel}: {toGujaratiNumber(school.schoolCode)}
              </div>
            </div>
          </div>
        </div>

        {/* Bottom Strip */}
        <div className="mt-6 flex flex-col items-center justify-between gap-3 border-t border-slate-100 pt-5 text-xs text-slate-500 sm:flex-row">
          <div className="space-y-1 text-center sm:text-left">
            <p className="text-slate-500">
              © 2026 Rakesh Patel, Principal at PM SHRI Dhadhana Primary School. All rights
              reserved.
            </p>
            <p className="text-slate-500">Built and maintained by Raj Patel | Rorfost</p>
          </div>
          <div className="flex items-center gap-4">
            <Link
              to="/admin/login"
              className="inline-flex items-center gap-1.5 text-slate-500 hover:text-slate-800 transition-colors"
            >
              <Lock size={12} aria-hidden="true" />
              <span>Admin Login</span>
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
