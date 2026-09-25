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
    <header className="sticky top-0 z-40 w-full border-b border-slate-200 bg-white/95 shadow-sm backdrop-blur-sm">
      <div className="flex min-h-[64px] items-center gap-3 px-4 sm:px-5">

        {/* Mobile hamburger */}
        <button
          type="button"
          onClick={onMenuClick}
          aria-label="મેનૂ ખોલો"
          className="flex size-10 shrink-0 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-700 lg:hidden"
        >
          <Menu size={22} aria-hidden="true" />
        </button>

        {/* Logo + school info — appears once only */}
        <Link
          to="/"
          className="flex flex-1 items-center gap-3 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-700"
        >
          <img
            src={school.logoUrl ?? schoolLogo}
            onError={(e) => {
              e.currentTarget.onerror = null;
              e.currentTarget.src = schoolLogo;
            }}
            alt="શાળા લોગો"
            className="size-10 sm:size-12 shrink-0 rounded-full border-2 border-blue-100 object-contain shadow-sm"
          />

          <div className="flex min-w-0 flex-col">
            <span className="line-clamp-1 text-sm font-bold leading-snug text-slate-900 sm:text-base lg:text-lg">
              {school.name}
            </span>
            <span className="line-clamp-1 text-[10px] text-slate-500 sm:text-xs">
              તા. સમી, જિ. પાટણ
              {school.establishedYear
                ? ` · ${LABELS.estLabel}: ${toGujaratiNumber(school.establishedYear)}`
                : ""}
              {school.schoolCode
                ? ` · ${LABELS.diseLabel}: ${toGujaratiNumber(school.schoolCode)}`
                : ""}
            </span>
          </div>
        </Link>
      </div>
    </header>
  );
}
