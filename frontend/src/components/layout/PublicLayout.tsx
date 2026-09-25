import { useEffect, useState } from "react";
import { Outlet, useLocation, NavLink } from "react-router-dom";
import { Footer } from "@/components/layout/Footer";
import { Header, NAV_LINKS } from "@/components/layout/Header";
import { SkipToContent } from "@/components/common/SkipToContent";
import {
  BookOpen,
  Calculator,
  Camera,
  GraduationCap,
  Home,
  Info,
  Mail,
  Newspaper,
  Phone,
  UserCheck,
  X,
} from "lucide-react";
import { LABELS } from "@/utils/gujarati";

// Icon mapped to each NAV_LINKS entry by index
const NAV_ICONS = [Home, Info, UserCheck, Newspaper, Camera, Phone];

const STUDENT_LINKS = [
  { to: "/student/materials", icon: BookOpen, label: LABELS.materials },
  { to: "/tools", icon: Calculator, label: LABELS.tools },
  { to: "/student/results", icon: GraduationCap, label: LABELS.results },
];

// Desktop sidebar
function PublicSidebar() {
  return (
    <nav className="flex flex-col gap-0.5 px-2 py-4" aria-label="મુખ્ય નેવ">
      {NAV_LINKS.map((item, idx) => {
        const Icon = NAV_ICONS[idx];
        return (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === "/"}
            className={({ isActive }) =>
              `group flex min-h-[42px] items-center gap-3 rounded-xl px-3.5 text-sm font-medium transition-all duration-150 ${
                isActive
                  ? "bg-[#0d2461] text-white shadow-md"
                  : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
              }`
            }
          >
            {({ isActive }) => (
              <>
                <Icon
                  size={17}
                  strokeWidth={isActive ? 2.5 : 1.75}
                  className={`shrink-0 transition-colors ${isActive ? "text-white" : "text-slate-400 group-hover:text-slate-700"}`}
                  aria-hidden="true"
                />
                <span>{item.label}</span>
              </>
            )}
          </NavLink>
        );
      })}

      <div className="mx-3 mt-4 border-t border-slate-200" />

      <p className="px-3.5 pt-3 pb-1 text-[10px] font-bold uppercase tracking-widest text-slate-400">
        {LABELS.studentCorner}
      </p>

      {STUDENT_LINKS.map(({ to, icon: Icon, label }) => (
        <NavLink
          key={to}
          to={to}
          className={({ isActive }) =>
            `group flex min-h-[42px] items-center gap-3 rounded-xl px-3.5 text-sm font-medium transition-all duration-150 ${
              isActive
                ? "bg-[#0d2461] text-white shadow-md"
                : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
            }`
          }
        >
          {({ isActive }) => (
            <>
              <Icon
                size={17}
                strokeWidth={isActive ? 2.5 : 1.75}
                className={`shrink-0 transition-colors ${isActive ? "text-yellow-400" : "text-slate-400 group-hover:text-slate-700"}`}
                aria-hidden="true"
              />
              <span>{label}</span>
            </>
          )}
        </NavLink>
      ))}
    </nav>
  );
}

// Mobile slide-out drawer
const DRAWER_LINKS = [
  { to: "/", label: LABELS.home, icon: Home },
  { to: "/about", label: LABELS.about, icon: Info },
  { to: "/principal", label: LABELS.principalDesk, icon: UserCheck },
  { to: "/notices", label: LABELS.notices, icon: Newspaper },
  { to: "/gallery", label: LABELS.gallery, icon: Camera },
  { to: "/contact", label: LABELS.contact, icon: Mail },
  { to: "/student/materials", label: LABELS.materials, icon: BookOpen },
  { to: "/tools", label: LABELS.tools, icon: Calculator },
  { to: "/student/results", label: LABELS.results, icon: GraduationCap },
];

function MobileDrawer({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) {
  useEffect(() => {
    if (!isOpen) return;
    const handler = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 lg:hidden">
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        className="absolute inset-y-0 left-0 flex w-[80vw] max-w-sm flex-col bg-white shadow-2xl"
        role="dialog"
        aria-modal="true"
      >
        {/* Drawer header */}
        <div className="flex items-center justify-between bg-[#0d2461] px-4 py-4">
          <span className="text-base font-bold text-white">મેનૂ</span>
          <button
            onClick={onClose}
            className="flex size-9 items-center justify-center rounded-lg text-white/70 hover:bg-white/10"
            aria-label="બંધ"
          >
            <X size={20} />
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto p-3" aria-label="ડ્રોઅર">
          <ul className="space-y-0.5">
            {DRAWER_LINKS.map(({ to, label, icon: Icon }) => (
              <li key={to}>
                <NavLink
                  to={to}
                  end={to === "/"}
                  onClick={onClose}
                  className={({ isActive }) =>
                    `flex min-h-[48px] items-center gap-3.5 rounded-xl px-4 text-sm font-medium transition-colors ${
                      isActive
                        ? "bg-[#0d2461] text-white"
                        : "text-slate-700 hover:bg-slate-100"
                    }`
                  }
                >
                  {({ isActive }) => (
                    <>
                      <Icon
                        size={18}
                        className={`shrink-0 ${isActive ? "text-yellow-400" : "text-slate-400"}`}
                        aria-hidden="true"
                      />
                      {label}
                    </>
                  )}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      </div>
    </div>
  );
}

// ── Public Layout ──────────────────────────────────────────────────────────────
export function PublicLayout() {
  const location = useLocation();
  const [drawerOpen, setDrawerOpen] = useState(false);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, [location.pathname]);

  useEffect(() => {
    setDrawerOpen(false);
  }, [location.pathname]);

  return (
    <div className="flex min-h-dvh w-full max-w-full flex-col overflow-x-hidden bg-slate-50 text-slate-900">
      <SkipToContent targetId="main-content" />

      {/* Fixed top header */}
      <Header onMenuClick={() => setDrawerOpen(true)} />
      {/* Spacer so content starts below fixed header */}
      <div className="h-[68px] sm:h-[76px] shrink-0" aria-hidden="true" />

      {/* Body: sidebar + content, together */}
      <div className="flex w-full max-w-full min-w-0 flex-1">
        {/* Desktop sidebar */}
        <aside className="hidden w-56 shrink-0 border-r border-slate-200 bg-white lg:block xl:w-64">
          <div className="sticky top-[76px] max-h-[calc(100dvh-76px)] overflow-y-auto">
            <PublicSidebar />
          </div>
        </aside>

        {/* Main scrollable content */}
        <main
          id="main-content"
          tabIndex={-1}
          className="w-full max-w-full min-w-0 flex-1 focus:outline-none overflow-x-hidden"
        >
          <div className="w-full max-w-full px-3.5 py-5 sm:px-6 sm:py-8">
            <Outlet />
          </div>
        </main>
      </div>

      {/* Full-width footer */}
      <Footer />

      {/* Mobile nav elements */}
      <MobileDrawer isOpen={drawerOpen} onClose={() => setDrawerOpen(false)} />
    </div>
  );
}
