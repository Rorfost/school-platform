import { useState } from "react";
import { Menu } from "lucide-react";
import { Link, NavLink } from "react-router-dom";
import schoolLogo from "@/assets/school-logo.jpeg";
import { Badge } from "@/components/ui/Badge";
import { MobileNav } from "@/components/layout/MobileNav";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";
import { LABELS, toGujaratiNumber } from "@/utils/gujarati";

const NAV_LINKS = [
  { to: "/", label: LABELS.home },
  { to: "/about", label: LABELS.about },
  { to: "/principal", label: LABELS.principalDesk },
  { to: "/notices", label: LABELS.notices },
  { to: "/gallery", label: LABELS.gallery },
  { to: "/contact", label: LABELS.contact },
];

export function Header() {
  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const school = useEffectiveSchoolInfo();

  return (
    <>
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur-xs">
        {/* Top Info Strip */}
        <div className="border-b border-slate-100 bg-slate-50 px-4 py-1.5 text-xs text-slate-600 sm:px-6">
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-3">
            <div className="flex flex-wrap items-center gap-2 sm:gap-3">
              <Badge variant="primary" size="sm" className="font-semibold">
                {LABELS.diseLabel}: {toGujaratiNumber(school.schoolCode)}
              </Badge>
              <span className="hidden sm:inline text-slate-300">•</span>
              <span className="text-slate-600 font-medium hidden sm:inline">
                {LABELS.estLabel}: {toGujaratiNumber(school.establishedYear)}
              </span>
            </div>
            <div className="text-right">
              <span className="font-semibold text-blue-900 tracking-wide text-[11px] sm:text-xs">
                ॥ સા વિદ્યા યા વિમુક્તયે ॥
              </span>
            </div>
          </div>
        </div>

        {/* Main Header Bar */}
        <div className="mx-auto flex min-h-16 max-w-6xl items-center gap-3 px-4 py-2 sm:min-h-18 sm:px-6">
          {/* The left-aligned trigger follows the familiar mobile navigation convention. */}
          <button
            type="button"
            onClick={() => setIsMobileOpen(true)}
            aria-expanded={isMobileOpen}
            aria-label="મુખ્ય મેનૂ ખોલો"
            className="flex size-11 shrink-0 items-center justify-center rounded-lg text-slate-700 hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-900 lg:hidden"
          >
            <Menu size={24} aria-hidden="true" />
          </button>
          {/* Logo & School Name */}
          <Link
            to="/"
            className="ml-auto flex min-w-0 items-center justify-end gap-2.5 rounded-lg text-right focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-900 lg:ml-0 lg:justify-start lg:text-left"
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
            <div className="flex min-w-0 flex-col">
              <span className="line-clamp-2 text-sm font-bold leading-tight tracking-tight text-slate-900 sm:text-lg lg:text-xl">
                {school.name}
              </span>
              <span className="hidden text-xs font-medium text-slate-500 lg:block">
                {LABELS.estLabel}: {toGujaratiNumber(school.establishedYear)} · {LABELS.diseLabel}:{" "}
                {toGujaratiNumber(school.schoolCode)}
              </span>
            </div>
          </Link>

          {/* Desktop Navigation */}
          <nav aria-label="મુખ્ય માર્ગદર્શન" className="ml-auto hidden items-center gap-1 lg:flex">
            {NAV_LINKS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `inline-flex min-h-10 items-center rounded-lg px-3.5 py-2 text-sm font-medium transition-colors ${
                    isActive
                      ? "bg-blue-50 text-blue-900 font-semibold"
                      : "text-slate-700 hover:bg-slate-100 hover:text-slate-900"
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

        </div>
      </header>

      {/* Mobile Drawer */}
      <MobileNav isOpen={isMobileOpen} onClose={() => setIsMobileOpen(false)} />
    </>
  );
}
