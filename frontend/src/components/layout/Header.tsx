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

  const estText = school.establishedYear
    ? `${LABELS.estLabel}: ${toGujaratiNumber(school.establishedYear)}`
    : "";
  const diseText = school.schoolCode
    ? `${LABELS.diseLabel}: ${toGujaratiNumber(school.schoolCode)}`
    : "";

  return (
    <header className="fixed top-0 inset-x-0 z-40 w-full border-b border-slate-200 bg-white/95 shadow-xs backdrop-blur-sm">
      <div className="flex min-h-[64px] sm:min-h-[72px] items-center justify-between gap-3 px-3.5 py-2 sm:px-6">

        {/* Left Side: Mobile Hamburger + Logo + School Info */}
        <div className="flex min-w-0 flex-1 items-center gap-2 sm:gap-3.5 overflow-hidden">
          {/* Mobile hamburger */}
          <button
            type="button"
            onClick={onMenuClick}
            aria-label="મેનૂ ખોલો"
            className="flex size-9 sm:size-10 shrink-0 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-700 lg:hidden"
          >
            <Menu size={22} aria-hidden="true" />
          </button>

          <Link
            to="/"
            className="flex min-w-0 flex-1 items-center gap-2 sm:gap-3 rounded-lg overflow-hidden focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-700"
          >
            <img
              src={school.logoUrl ?? schoolLogo}
              onError={(e) => {
                e.currentTarget.onerror = null;
                e.currentTarget.src = schoolLogo;
              }}
              alt="શાળા લોગો"
              className="size-9 sm:size-12 shrink-0 rounded-full border-2 border-blue-100 object-contain shadow-sm"
            />

            <div className="flex min-w-0 flex-1 flex-col leading-tight overflow-hidden">
              {/* Line 1: School Name */}
              <span className="truncate text-xs sm:text-base lg:text-lg font-bold text-slate-900">
                {school.name}
              </span>

              {/* Line 2: Location */}
              <span className="truncate text-[10px] sm:text-xs font-semibold text-slate-600">
                {school.address}
              </span>

              {/* Line 3 (Mobile only): EST Year & DISE Code */}
              <span className="truncate text-[9px] sm:hidden font-medium text-slate-500">
                {[estText, diseText].filter(Boolean).join(" · ")}
              </span>
            </div>
          </Link>
        </div>

        {/* Right Side (Laptop/Desktop view): 2 lines for EST Year & DISE Code */}
        <div className="hidden sm:flex shrink-0 flex-col text-right text-xs text-slate-500 font-medium leading-snug border-l border-slate-200 pl-4">
          {estText && <span>{estText}</span>}
          {diseText && <span>{diseText}</span>}
        </div>

      </div>
    </header>
  );
}
