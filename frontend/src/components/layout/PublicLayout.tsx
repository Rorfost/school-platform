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
  UserCheck,
  Menu,
  X,
} from "lucide-react";
import { LABELS } from "@/utils/gujarati";

// Desktop sidebar navigation
function PublicSidebar() {
  return (
    <nav className="flex flex-col gap-1" aria-label="મુખ્ય માર્ગદર્શન">
      {NAV_LINKS.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === "/"}
          className={({ isActive }) =>
            `flex min-h-10 items-center rounded-xl px-3.5 py-2.5 text-sm font-medium transition-all ${
              isActive
                ? "bg-blue-700 text-white shadow-sm font-semibold"
                : "text-slate-700 hover:bg-slate-100 hover:text-slate-900"
            }`
          }
        >
          {item.label}
        </NavLink>
      ))}

      <div className="mt-5 border-t border-slate-200 pt-5">
        <p className="px-3.5 text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
          {LABELS.studentCorner}
        </p>
        <div className="flex flex-col gap-1">
          <NavLink
            to="/student/materials"
            className={({ isActive }) =>
              `flex min-h-10 items-center gap-3 rounded-xl px-3.5 text-sm transition-all ${
                isActive
                  ? "bg-blue-700 text-white font-semibold"
                  : "text-slate-700 hover:bg-slate-100"
              }`
            }
          >
            <BookOpen size={16} className="shrink-0" />
            <span>{LABELS.materials}</span>
          </NavLink>
          <NavLink
            to="/tools"
            className={({ isActive }) =>
              `flex min-h-10 items-center gap-3 rounded-xl px-3.5 text-sm transition-all ${
                isActive
                  ? "bg-blue-700 text-white font-semibold"
                  : "text-slate-700 hover:bg-slate-100"
              }`
            }
          >
            <Calculator size={16} className="shrink-0" />
            <span>{LABELS.tools}</span>
          </NavLink>
          <NavLink
            to="/student/results"
            className={({ isActive }) =>
              `flex min-h-10 items-center gap-3 rounded-xl px-3.5 text-sm transition-all ${
                isActive
                  ? "bg-blue-700 text-white font-semibold"
                  : "text-slate-700 hover:bg-slate-100"
              }`
            }
          >
            <GraduationCap size={16} className="shrink-0" />
            <span>{LABELS.results}</span>
          </NavLink>
        </div>
      </div>
    </nav>
  );
}

// Mobile bottom tab bar — thumb-friendly, replaces hamburger as primary nav for mobile
const BOTTOM_TABS = [
  { to: "/", label: LABELS.home, icon: Home, end: true },
  { to: "/notices", label: LABELS.notices, icon: Newspaper, end: false },
  { to: "/gallery", label: LABELS.gallery, icon: Camera, end: false },
  { to: "/student/results", label: "પરિણામ", icon: GraduationCap, end: false },
  { to: "/contact", label: LABELS.contact, icon: Mail, end: false },
];

function MobileBottomNav() {
  return (
    <nav
      className="fixed bottom-0 inset-x-0 z-40 border-t border-slate-200 bg-white/95 backdrop-blur-sm lg:hidden safe-area-bottom"
      aria-label="ટેબ નેવિગેશન"
    >
      <div className="flex items-stretch">
        {BOTTOM_TABS.map((tab) => {
          const Icon = tab.icon;
          return (
            <NavLink
              key={tab.to}
              to={tab.to}
              end={tab.end}
              className={({ isActive }) =>
                `flex flex-1 flex-col items-center justify-center gap-0.5 py-2 px-1 text-[10px] font-medium transition-colors min-h-[56px] ${
                  isActive ? "text-blue-800" : "text-slate-500"
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <div
                    className={`rounded-lg p-1 transition-colors ${
                      isActive ? "bg-blue-50" : ""
                    }`}
                  >
                    <Icon
                      size={22}
                      strokeWidth={isActive ? 2.5 : 1.75}
                      aria-hidden="true"
                    />
                  </div>
                  <span>{tab.label}</span>
                </>
              )}
            </NavLink>
          );
        })}
      </div>
    </nav>
  );
}

// Mobile slide-out drawer for "More" links (about, principal, etc.)
function MobileDrawer({
  isOpen,
  onClose,
}: {
  isOpen: boolean;
  onClose: () => void;
}) {
  useEffect(() => {
    if (!isOpen) return;
    const handle = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", handle);
    return () => window.removeEventListener("keydown", handle);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const ALL_LINKS = [
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

  return (
    <div className="fixed inset-0 z-50 lg:hidden">
      <div
        className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm"
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        className="fixed inset-y-0 left-0 flex w-[85vw] max-w-sm flex-col bg-white shadow-2xl"
        role="dialog"
        aria-modal="true"
        aria-label="બધા મેનૂ"
      >
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <span className="text-base font-bold text-slate-900">મેનૂ</span>
          <button
            type="button"
            onClick={onClose}
            aria-label="મેનૂ બંધ કરો"
            className="flex size-10 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-100"
          >
            <X size={22} aria-hidden="true" />
          </button>
        </div>
        <nav className="flex-1 overflow-y-auto p-4" aria-label="ડ્રોઅર નેવ">
          <ul className="space-y-1">
            {ALL_LINKS.map((item) => {
              const Icon = item.icon;
              return (
                <li key={item.to}>
                  <NavLink
                    to={item.to}
                    end={item.to === "/"}
                    onClick={onClose}
                    className={({ isActive }) =>
                      `flex min-h-12 items-center gap-3.5 rounded-xl px-4 text-base font-medium transition-colors ${
                        isActive
                          ? "bg-blue-700 text-white"
                          : "text-slate-700 hover:bg-slate-50"
                      }`
                    }
                  >
                    <Icon size={20} className="shrink-0" aria-hidden="true" />
                    <span>{item.label}</span>
                  </NavLink>
                </li>
              );
            })}
          </ul>
        </nav>
      </div>
    </div>
  );
}

export function PublicLayout() {
  const location = useLocation();
  const [drawerOpen, setDrawerOpen] = useState(false);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, [location.pathname]);

  // Close drawer on route change
  useEffect(() => {
    setDrawerOpen(false);
  }, [location.pathname]);

  return (
    // Full-viewport shell — NO max-width cap here so header + footer go edge-to-edge
    <div className="flex min-h-dvh flex-col bg-stone-50 text-slate-900">
      <SkipToContent targetId="main-content" />

      {/* Sticky full-width header */}
      <Header onMenuClick={() => setDrawerOpen(true)} />

      {/* Sidebar + content row — fills available height, scrolls internally */}
      <div className="flex flex-1">
        {/* Desktop sidebar: matches header logo zone widths exactly */}
        <aside className="hidden w-60 shrink-0 sticky top-16 self-start max-h-[calc(100dvh-64px)] overflow-y-auto border-r border-slate-200 bg-white px-3 py-5 lg:block xl:w-72">
          <PublicSidebar />
        </aside>

        {/* Scrollable content column */}
        <main
          id="main-content"
          tabIndex={-1}
          className="min-w-0 flex-1 focus:outline-none"
        >
          {/* Inner content — full width, generous padding */}
          <div className="w-full px-4 py-6 sm:px-6 sm:py-8 pb-24 lg:pb-8">
            <Outlet />
          </div>
        </main>
      </div>

      {/* Footer spans 100% width — outside the flex row */}
      <Footer />

      {/* Mobile: extra bottom padding so content clears the fixed tab bar */}
      <div className="h-14 lg:hidden" aria-hidden="true" />

      {/* Mobile bottom tab bar */}
      <MobileBottomNav />

      {/* Mobile slide-out drawer */}
      <MobileDrawer isOpen={drawerOpen} onClose={() => setDrawerOpen(false)} />
    </div>
  );
}
