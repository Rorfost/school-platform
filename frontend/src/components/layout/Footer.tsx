import { useEffect, useState } from "react";
import { Eye, Lock, Mail, MapPin, Phone } from "lucide-react";
import { Link } from "react-router-dom";
import schoolLogo from "@/assets/school-logo.jpeg";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";
import { apiRequest } from "@/api/client";
import type { VisitResponse } from "@/api/types";
import { LABELS, toGujaratiNumber } from "@/utils/gujarati";

export function Footer() {
  const school = useEffectiveSchoolInfo();
  const [visits, setVisits] = useState<number | null>(null);



  useEffect(() => {
    const marker = "school-portal-visit-counted";
    const counted = window.sessionStorage.getItem(marker);
    const req = counted
      ? apiRequest<VisitResponse>("/api/v1/public/visits")
      : apiRequest<VisitResponse>("/api/v1/public/visits", { method: "POST", skipCsrf: true });

    void req
      .then((res) => {
        if (!counted) window.sessionStorage.setItem(marker, "1");
        setVisits(res.totalVisits);
      })
      .catch(() => undefined);
  }, []);

  return (
    <footer className="w-full border-t border-slate-200 bg-white text-slate-600">
      <div className="mx-auto flex max-w-4xl flex-col items-center gap-4 px-4 py-6 text-center">
        {/* School Logo & Brand Header */}
        <div className="flex flex-col items-center gap-2">
          <img
            src={school.logoUrl ?? schoolLogo}
            onError={(e) => {
              e.currentTarget.onerror = null;
              e.currentTarget.src = schoolLogo;
            }}
            alt="School logo"
            className="size-12 shrink-0 rounded-full border-2 border-blue-100 object-contain shadow-sm"
          />
          <div>
            <h3 className="text-sm font-bold text-slate-900 sm:text-base">{school.name}</h3>
            <p className="text-xs text-slate-500 font-medium">
              પ્રાથમિક શિક્ષણ દ્વારા બાળકોમાં સર્વાંગી વિકાસ અને સંસ્કાર સિંચન
            </p>
          </div>
          <p className="text-xs font-semibold text-blue-900">॥ સા વિદ્યા યા વિમુક્તયે ॥</p>
        </div>

        {/* Contact details - Compact centered list */}
        <div className="flex flex-col sm:flex-row sm:flex-wrap items-center justify-center gap-2 sm:gap-x-6 text-xs text-slate-600 pt-1 w-full max-w-full overflow-hidden">
          {school.address && (
            <div className="flex items-center gap-1.5 max-w-full">
              <MapPin size={14} className="shrink-0 text-blue-800" aria-hidden="true" />
              <span className="truncate">{school.address}</span>
            </div>
          )}

          {school.phone && (
            <div className="flex items-center gap-1.5 max-w-full">
              <Phone size={14} className="shrink-0 text-blue-800" aria-hidden="true" />
              <a
                href={`tel:${school.phone}`}
                className="hover:text-blue-900 transition-colors truncate"
              >
                {school.phone}
              </a>
            </div>
          )}

          {school.email && (
            <div className="flex items-center gap-1.5 max-w-full min-w-0">
              <Mail size={14} className="shrink-0 text-blue-800" aria-hidden="true" />
              <a
                href={`mailto:${school.email}`}
                className="hover:text-blue-900 transition-colors break-all min-w-0"
              >
                {school.email}
              </a>
            </div>
          )}

          {school.schoolCode && (
            <span className="text-slate-500 shrink-0">
              {LABELS.diseLabel}: {toGujaratiNumber(school.schoolCode)}
            </span>
          )}
        </div>

        {/* Bottom copyright & admin link */}
        <div className="w-full border-t border-slate-100 pt-3 text-[11px] text-slate-500 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="text-center sm:text-left">
            <p>© 2026 Rakesh Patel, Principal at PM Shri Dhadhana Primary School.</p>
            <p className="text-slate-500">Built & Maintained by Raj Patel | Rorfost</p>
          </div>

          <div className="flex items-center gap-3">
            {
              <span className="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-2.5 py-0.5 text-[11px] font-semibold text-blue-900 border border-blue-200">
                <Eye size={12} className="text-blue-800" aria-hidden="true" />
                <span>Visits: {visits == null ? 0 : visits}</span>
              </span>
            }
            <Link
              to="/admin/login"
              className="inline-flex items-center gap-1.5 text-slate-500 hover:text-blue-900 font-medium transition-colors"
            >
              <Lock size={11} aria-hidden="true" />
              <span>Admin Login</span>
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
