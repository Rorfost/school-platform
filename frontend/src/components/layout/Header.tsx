import { Menu } from "lucide-react";
import { Link } from "react-router-dom";
import schoolLogo from "@/assets/school-logo.jpeg";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";
import { LABELS, toGujaratiNumber } from "@/utils/gujarati";

export const NAV_LINKS = [
  { to: "/", label: LABELS.home },
  { to: "/about", label: LABELS.about },
  { to: "/principal", label: LABELS.principalDesk },
  { to: "/notices", label: LABELS.notices },
  { to: "/gallery", label: LABELS.gallery },
  { to: "/contact", label: LABELS.contact },
];

export interface HeaderProps {
  onMenuClick?: () => void;
}

export function Header({ onMenuClick }: HeaderProps = {}) {
  const school = useEffectiveSchoolInfo();

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-200 bg-white/95 backdrop-blur-sm">
      {/*
        Two-zone layout on desktop:
          LEFT  — same width as the sidebar (w-60 / xl:w-72) + border-r
          RIGHT — flexible, shows full school info

        On mobile: single row with hamburger + logo + name (no zone split)
      */}
      <div className="flex min-h-16 items-stretch">

        {/* ── LOGO ZONE (desktop left column, matches sidebar width) ── */}
        <div className="hidden lg:flex w-60 xl:w-72 shrink-0 items-center gap-2.5 border-r border-slate-200 bg-white px-4 py-2">
          <Link
            to="/"
            className="flex items-center gap-2.5 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-700"
          >
            <img
              src={school.logoUrl ?? schoolLogo}
              onError={(e) => {
                e.currentTarget.onerror = null;
                e.currentTarget.src = schoolLogo;
              }}
              alt="શાળા લોગો"
              className="size-10 shrink-0 rounded-full border-2 border-blue-100 object-contain shadow-sm"
            />
            <span className="text-sm font-bold leading-snug text-slate-900 line-clamp-2">
              {school.name}
            </span>
          </Link>
        </div>

        {/* ── SCHOOL INFO ZONE (desktop right column / full mobile) ── */}
        <div className="flex flex-1 items-center gap-3 px-4 py-2 sm:px-5">
          {/* Mobile hamburger */}
          <button
            type="button"
            onClick={onMenuClick}
            aria-label="બધા મેનૂ ખોલો"
            className="flex size-10 shrink-0 items-center justify-center rounded-lg text-slate-700 hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-700 lg:hidden"
          >
            <Menu size={22} aria-hidden="true" />
          </button>

          {/* Mobile-only logo */}
          <Link
            to="/"
            className="flex items-center gap-2.5 lg:hidden focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-700 rounded-lg"
          >
            <img
              src={school.logoUrl ?? schoolLogo}
              onError={(e) => {
                e.currentTarget.onerror = null;
                e.currentTarget.src = schoolLogo;
              }}
              alt="શાળા લોગો"
              className="size-10 shrink-0 rounded-full border-2 border-blue-100 object-contain shadow-sm"
            />
          </Link>

          {/* School name + subtitle — visible on all screen sizes */}
          <div className="flex min-w-0 flex-1 flex-col">
            <Link to="/" className="focus-visible:outline-none">
              <span className="line-clamp-1 text-base font-bold leading-tight text-slate-900 sm:text-lg lg:text-xl">
                {school.name}
              </span>
            </Link>
            <span className="line-clamp-1 text-[11px] font-medium text-slate-500 sm:text-xs">
              તા. સમી, જિ. પાટણ&nbsp;·&nbsp;{LABELS.estLabel}:&nbsp;
              {toGujaratiNumber(school.establishedYear || "")}&nbsp;·&nbsp;
              {LABELS.diseLabel}:&nbsp;{toGujaratiNumber(school.schoolCode || "")}
            </span>
          </div>
        </div>

      </div>
    </header>
  );
}
