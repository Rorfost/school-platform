import { useEffect } from "react";
import {
  BookOpen,
  Camera,
  GraduationCap,
  Home,
  Info,
  Mail,
  Newspaper,
  UserCheck,
  X,
} from "lucide-react";
import { NavLink } from "react-router-dom";
import { LABELS } from "@/utils/gujarati";

export interface MobileNavProps {
  isOpen: boolean;
  onClose: () => void;
}

const NAV_ITEMS = [
  { to: "/", label: LABELS.home, icon: Home },
  { to: "/about", label: LABELS.about, icon: Info },
  { to: "/principal", label: LABELS.principalDesk, icon: UserCheck },
  { to: "/notices", label: LABELS.notices, icon: Newspaper },
  { to: "/gallery", label: LABELS.gallery, icon: Camera },
  { to: "/contact", label: LABELS.contact, icon: Mail },
];

export function MobileNav({ isOpen, onClose }: MobileNavProps) {
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden";
      const handleKeyDown = (event: KeyboardEvent) => {
        if (event.key === "Escape") {
          onClose();
        }
      };
      window.addEventListener("keydown", handleKeyDown);
      return () => {
        document.body.style.overflow = "";
        window.removeEventListener("keydown", handleKeyDown);
      };
    }
  }, [isOpen, onClose]);

  if (!isOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 md:hidden">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Slide-out Menu */}
      <div
        className="fixed inset-y-0 right-0 w-full max-w-xs bg-white shadow-xl flex flex-col"
        role="dialog"
        aria-modal="true"
        aria-label="મુખ્ય મેનૂ"
      >
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <span className="text-base font-semibold text-slate-900">મેનૂ</span>
          <button
            type="button"
            onClick={onClose}
            aria-label="મેનૂ બંધ કરો"
            className="flex size-11 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-100 hover:text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-900"
          >
            <X size={22} aria-hidden="true" />
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto p-4" aria-label="મોબાઈલ માર્ગદર્શન">
          <ul className="space-y-1.5">
            {NAV_ITEMS.map((item) => {
              const Icon = item.icon;
              return (
                <li key={item.to}>
                  <NavLink
                    to={item.to}
                    onClick={onClose}
                    className={({ isActive }) =>
                      `flex min-h-12 items-center gap-3.5 rounded-xl px-4 text-base font-medium transition-colors ${
                        isActive
                          ? "bg-blue-50 text-blue-900 font-semibold"
                          : "text-slate-700 hover:bg-slate-50 hover:text-slate-950"
                      }`
                    }
                  >
                    <Icon size={20} className="shrink-0 text-blue-900" aria-hidden="true" />
                    <span>{item.label}</span>
                  </NavLink>
                </li>
              );
            })}
          </ul>

          <div className="mt-6 border-t border-slate-200 pt-5">
            <p className="px-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">
              {LABELS.studentCorner}
            </p>
            <ul className="mt-2 space-y-1">
              <li>
                <NavLink
                  to="/student/materials"
                  onClick={onClose}
                  className="flex min-h-11 items-center gap-3 rounded-lg px-4 text-sm text-slate-700 hover:bg-slate-50"
                >
                  <BookOpen size={18} className="text-slate-500" aria-hidden="true" />
                  <span>{LABELS.materials}</span>
                </NavLink>
              </li>
              <li>
                <NavLink
                  to="/student/results"
                  onClick={onClose}
                  className="flex min-h-11 items-center gap-3 rounded-lg px-4 text-sm text-slate-700 hover:bg-slate-50"
                >
                  <GraduationCap size={18} className="text-slate-500" aria-hidden="true" />
                  <span>{LABELS.results}</span>
                </NavLink>
              </li>
            </ul>
          </div>
        </nav>
      </div>
    </div>
  );
}
