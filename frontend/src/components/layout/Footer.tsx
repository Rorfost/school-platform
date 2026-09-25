import { useEffect, useState } from "react";
import { Lock, Mail, MapPin } from "lucide-react";
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
    const request = counted
      ? apiRequest<VisitResponse>("/api/v1/public/visits")
      : apiRequest<VisitResponse>("/api/v1/public/visits", { method: "POST", skipCsrf: true });

    void request
      .then((response) => {
        if (!counted) window.sessionStorage.setItem(marker, "1");
        setVisits(response.totalVisits);
      })
      .catch(() => undefined);
  }, []);

  return (
    <footer className="w-full border-t border-slate-200 bg-white text-slate-700">
      {/*
        Two-zone layout on desktop to mirror the header + sidebar:
          LEFT  — same width as sidebar (w-60 / xl:w-72) + border-r — school brand
          RIGHT — contact info, links, copyright
        On mobile: single column.
      */}
      <div className="flex flex-col lg:flex-row">

        {/* ── SIDEBAR-ALIGNED BRAND ZONE ── */}
        <div className="flex w-full shrink-0 flex-col gap-3 border-b border-slate-100 px-5 py-6 lg:w-60 lg:border-b-0 lg:border-r lg:border-slate-200 xl:w-72">
          <div className="flex items-center gap-3">
            <img
              src={school.logoUrl ?? schoolLogo}
              onError={(e) => {
                e.currentTarget.onerror = null;
                e.currentTarget.src = schoolLogo;
              }}
              alt="શાળા લોગો"
              className="size-12 rounded-full border-2 border-blue-100 object-contain shadow-sm shrink-0"
            />
            <div>
              <h3 className="text-sm font-bold leading-snug text-slate-900">{school.name}</h3>
              <p className="mt-0.5 text-[11px] text-slate-500 font-medium">
                {LABELS.estLabel}: {toGujaratiNumber(school.establishedYear || "")}
              </p>
            </div>
          </div>
          <p className="text-xs text-slate-600 leading-relaxed">
            પ્રાથમિક શિક્ષણ દ્વારા બાળકોમાં સર્વાંગી વિકાસ અને સંસ્કાર સિંચન.
          </p>
          <p className="text-xs font-bold text-blue-900">॥ સા વિદ્યા યા વિમુક્તયે ॥</p>
        </div>

        {/* ── MAIN FOOTER CONTENT ZONE ── */}
        <div className="flex flex-1 flex-col justify-between px-5 py-6 sm:px-7">
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
            {/* Contact */}
            <div>
              <h4 className="mb-3 text-sm font-semibold text-slate-900">{LABELS.contact}</h4>
              <div className="space-y-2 text-xs text-slate-600">
                {school.address && (
                  <div className="flex items-start gap-2">
                    <MapPin size={14} className="mt-0.5 shrink-0 text-blue-800" aria-hidden="true" />
                    <span>{school.address}</span>
                  </div>
                )}
                {school.email && (
                  <div className="flex items-center gap-2">
                    <Mail size={14} className="shrink-0 text-blue-800" aria-hidden="true" />
                    <a
                      href={`mailto:${school.email}`}
                      className="hover:text-blue-900 transition-colors break-all"
                    >
                      {school.email}
                    </a>
                  </div>
                )}
                <p className="text-slate-400">
                  {LABELS.diseLabel}: {toGujaratiNumber(school.schoolCode || "")}
                </p>
              </div>
            </div>

            {/* Quick links */}
            <div>
              <h4 className="mb-3 text-sm font-semibold text-slate-900">ઝડપી લિંક</h4>
              <ul className="space-y-1.5 text-xs text-slate-600">
                {[
                  { to: "/notices", label: LABELS.notices },
                  { to: "/gallery", label: LABELS.gallery },
                  { to: "/student/materials", label: LABELS.materials },
                  { to: "/student/results", label: LABELS.results },
                  { to: "/contact", label: LABELS.contact },
                ].map((link) => (
                  <li key={link.to}>
                    <Link
                      to={link.to}
                      className="hover:text-blue-900 transition-colors"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          {/* Bottom strip */}
          <div className="mt-6 flex flex-col gap-2 border-t border-slate-100 pt-4 text-[11px] text-slate-400 sm:flex-row sm:items-center sm:justify-between">
            <div className="space-y-0.5">
              <p>© 2026 Rakesh Patel, Principal — PM SHRI Dhadhana Primary School</p>
              <p>Built and maintained by Raj Patel | Rorfost</p>
            </div>
            <div className="flex items-center gap-4">
              {visits !== null && (
                <span>Visits: {visits.toLocaleString("en-IN")}</span>
              )}
              <Link
                to="/admin/login"
                className="inline-flex items-center gap-1.5 text-slate-400 hover:text-slate-700 transition-colors"
              >
                <Lock size={11} aria-hidden="true" />
                <span>Admin</span>
              </Link>
            </div>
          </div>
        </div>

      </div>
    </footer>
  );
}
