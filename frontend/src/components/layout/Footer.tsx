import { useEffect, useState } from "react";
import { Lock, Mail, MapPin, Phone } from "lucide-react";
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
    <footer className="w-full bg-slate-900 text-slate-300">

      {/* ── Main content ── */}
      <div className="mx-auto w-full px-5 py-10 sm:px-8">
        <div className="flex flex-col gap-10 md:flex-row md:gap-16">

          {/* Brand */}
          <div className="flex flex-col gap-4 md:max-w-xs">
            <div className="flex items-center gap-3">
              <div className="size-14 shrink-0 overflow-hidden rounded-full border-2 border-white/20 bg-white/10">
                <img
                  src={school.logoUrl ?? schoolLogo}
                  onError={(e) => {
                    e.currentTarget.onerror = null;
                    e.currentTarget.src = schoolLogo;
                  }}
                  alt="School logo"
                  className="size-full object-contain"
                />
              </div>
              <div>
                <h3 className="text-sm font-bold leading-snug text-white">{school.name}</h3>
                <p className="mt-0.5 text-xs text-slate-400">
                  {LABELS.estLabel}: {toGujaratiNumber(school.establishedYear || "")}
                </p>
              </div>
            </div>

            <p className="text-sm leading-relaxed text-slate-400">
              પ્રાથમિક શિક્ષણ દ્વારા બાળકોમાં સર્વાંગી વિકાસ અને સંસ્કાર સિંચન.
            </p>

            <p className="text-sm font-semibold text-white/80">
              ॥ સા વિદ્યા યા વિમુક્તયે ॥
            </p>
          </div>

          {/* Contact */}
          <div className="flex flex-col gap-3">
            <h4 className="text-xs font-bold uppercase tracking-widest text-slate-500">
              {LABELS.contact}
            </h4>

            <div className="flex flex-col gap-3 text-sm">
              {school.address && (
                <div className="flex items-start gap-2.5">
                  <MapPin size={15} className="mt-0.5 shrink-0 text-slate-500" />
                  <span className="leading-relaxed">{school.address}</span>
                </div>
              )}
              {school.phone && (
                <div className="flex items-center gap-2.5">
                  <Phone size={15} className="shrink-0 text-slate-500" />
                  <a href={`tel:${school.phone}`} className="hover:text-white transition-colors">
                    {school.phone}
                  </a>
                </div>
              )}
              {school.email && (
                <div className="flex items-center gap-2.5">
                  <Mail size={15} className="shrink-0 text-slate-500" />
                  <a
                    href={`mailto:${school.email}`}
                    className="hover:text-white transition-colors break-all"
                  >
                    {school.email}
                  </a>
                </div>
              )}
              {school.schoolCode && (
                <p className="text-xs text-slate-500 pt-1">
                  {LABELS.diseLabel}: {toGujaratiNumber(school.schoolCode)}
                </p>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* ── Bottom bar ── */}
      <div className="border-t border-white/10 px-5 py-4 sm:px-8">
        <div className="flex flex-col items-center justify-between gap-2 text-[11px] text-slate-500 sm:flex-row">
          <div className="space-y-0.5 text-center sm:text-left">
            <p>© 2026 Rakesh Patel — PM SHRI Dhadhana Primary School</p>
            <p>Built and maintained by Raj Patel · Rorfost</p>
          </div>

          <div className="flex items-center gap-4">
            {visits !== null && (
              <span>Visits: {visits.toLocaleString("en-IN")}</span>
            )}
            <Link
              to="/admin/login"
              className="inline-flex items-center gap-1.5 text-slate-500 hover:text-slate-300 transition-colors"
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
