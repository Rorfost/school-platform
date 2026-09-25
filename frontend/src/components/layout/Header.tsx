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
    <>
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur-xs">
        {/* Main Header Bar */}
        <div className="mx-auto flex min-h-16 w-full max-w-screen-xl items-center gap-3 px-4 py-2 sm:min-h-18 sm:px-6 lg:px-10">
          <button
            type="button"
            onClick={onMenuClick}
            aria-label="બધા મેનૂ ખોલો"
            className="flex size-11 shrink-0 items-center justify-center rounded-lg text-slate-700 hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-900 lg:hidden"
          >
            <Menu size={24} aria-hidden="true" />
          </button>
          
          <Link
            to="/"
            className="flex min-w-0 flex-1 items-center gap-3 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-900 text-left"
          >
            <img
              src={school.logoUrl ?? schoolLogo}
              onError={(event) => {
                event.currentTarget.onerror = null;
                event.currentTarget.src = schoolLogo;
              }}
              alt="શાળા લોગો"
              className="size-11 shrink-0 rounded-full border border-blue-100 object-contain shadow-xs lg:size-14"
            />
            <div className="flex min-w-0 flex-col flex-1">
              <span className="line-clamp-2 text-sm font-bold leading-tight tracking-tight text-slate-900 sm:text-lg lg:text-xl">
                {school.name}
              </span>
              <span className="text-xs font-medium text-slate-600 block">
                તા. સમી જિ. પાટણ · {LABELS.estLabel}: {toGujaratiNumber(school.establishedYear || "")} · {LABELS.diseLabel}:{" "}
                {toGujaratiNumber(school.schoolCode || "")}
              </span>
            </div>
          </Link>
        </div>
      </header>
    </>
  );
}
