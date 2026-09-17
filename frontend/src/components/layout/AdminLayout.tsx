import { useEffect, useRef, useState } from "react";
import {
  AlertCircle,
  Award,
  Camera,
  ExternalLink,
  FileText,
  FolderDown,
  GraduationCap,
  Layers,
  LayoutDashboard,
  LogOut,
  Menu,
  Settings,
  ShieldCheck,
  UserCheck,
  UserCog,
  X,
} from "lucide-react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import schoolLogo from "@/assets/school-logo.jpeg";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/features/auth/useAuth";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";

interface NavGroup {
  title: string;
  items: {
    to: string;
    label: string;
    icon: React.ElementType;
    end?: boolean;
  }[];
}

const NAV_GROUPS: NavGroup[] = [
  {
    title: "Overview",
    items: [{ to: "/admin", label: "Dashboard", icon: LayoutDashboard, end: true }],
  },
  {
    title: "Management",
    items: [
      { to: "/admin/assessments", label: "Results & Marks", icon: Award },
      { to: "/admin/materials", label: "Study Materials", icon: GraduationCap },
      { to: "/admin/notices", label: "Notices & Circulars", icon: FileText },
      { to: "/admin/gallery", label: "Photo Gallery", icon: Camera },
      { to: "/admin/downloads", label: "Downloads", icon: FolderDown },
    ],
  },
  {
    title: "Academics",
    items: [{ to: "/admin/academics", label: "Academic Setup", icon: Layers }],
  },
  {
    title: "School Settings",
    items: [
      { to: "/admin/school", label: "School Identity", icon: Settings },
      { to: "/admin/principal", label: "Principal Profile", icon: UserCheck },
    ],
  },
  {
    title: "Account",
    items: [{ to: "/admin/account", label: "Account & Security", icon: UserCog }],
  },
];

export function AdminLayout() {
  const { principal, logout } = useAuth();
  const school = useEffectiveSchoolInfo();
  const navigate = useNavigate();
  const location = useLocation();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const mainContentRef = useRef<HTMLElement>(null);

  useEffect(() => {
    mainContentRef.current?.scrollTo?.({ top: 0 });
  }, [location.pathname]);

  useEffect(() => {
    if (!isMobileMenuOpen) return;
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = originalOverflow;
    };
  }, [isMobileMenuOpen]);

  const handleLogout = async () => {
    await logout();
    navigate("/admin/login");
  };

  const renderNavLinks = (onItemClick?: () => void) => (
    <div className="space-y-6">
      {NAV_GROUPS.map((group) => (
        <div key={group.title}>
          <div className="px-3 text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
            {group.title}
          </div>
          <div className="space-y-1">
            {group.items.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  onClick={onItemClick}
                  className={({ isActive }) =>
                    `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                      isActive
                        ? "bg-blue-900 text-white font-semibold shadow-xs"
                        : "text-slate-700 hover:bg-slate-100 hover:text-slate-900"
                    }`
                  }
                >
                  <Icon size={18} aria-hidden="true" className="shrink-0" />
                  <span>{item.label}</span>
                </NavLink>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );

  return (
    <div className="flex h-dvh flex-col overflow-hidden bg-slate-100 text-slate-900">
      {/* Must Change Password Warning Banner */}
      {principal?.mustChangePassword && (
        <div className="flex shrink-0 items-center justify-between gap-3 bg-amber-600 px-4 py-2.5 text-xs font-semibold text-white shadow-sm sm:text-sm">
          <div className="flex items-center gap-2">
            <AlertCircle size={18} aria-hidden="true" className="shrink-0" />
            <span>Security Action Required: You must change your default password.</span>
          </div>
          <Link
            to="/admin/account"
            className="underline hover:text-amber-100 font-bold shrink-0 text-xs sm:text-sm"
          >
            Change Password Now
          </Link>
        </div>
      )}

      {/* Top Header */}
      <header className="z-30 shrink-0 border-b border-slate-200 bg-white shadow-xs">
        <div className="mx-auto flex min-h-16 items-center justify-between gap-4 px-4 sm:px-6">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="md:hidden p-2 text-slate-600 hover:text-slate-900 rounded-lg hover:bg-slate-100"
              aria-label="Toggle Navigation Menu"
            >
              {isMobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
            </button>

            <img
              src={school.logoUrl ?? schoolLogo}
              onError={(event) => {
                event.currentTarget.onerror = null;
                event.currentTarget.src = schoolLogo;
              }}
              alt="Logo"
              className="size-9 rounded-full object-contain border border-slate-200"
            />
            <div>
              <div className="flex items-center gap-2">
                <span className="text-sm sm:text-base font-bold text-slate-900">
                  Principal Admin Panel
                </span>
                <span className="hidden sm:inline-flex items-center gap-1 rounded-md bg-blue-50 px-2 py-0.5 text-xs font-semibold text-blue-900 border border-blue-200">
                  <ShieldCheck size={12} aria-hidden="true" />
                  <span>{principal?.role || "PRINCIPAL"}</span>
                </span>
              </div>
              <p className="text-xs text-slate-500 font-medium hidden sm:block">{school.name}</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <span className="hidden lg:inline-block text-xs text-slate-600 font-medium bg-slate-50 border border-slate-200 px-2.5 py-1 rounded-md">
              {principal?.email}
            </span>

            <Link
              to="/"
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1.5 text-xs font-medium text-slate-600 hover:text-slate-900 p-2 rounded-lg hover:bg-slate-50 transition-colors"
              title="Open public website in new tab"
            >
              <ExternalLink size={15} aria-hidden="true" />
              <span className="hidden sm:inline">View Public Site</span>
            </Link>

            <Button variant="outline" size="sm" onClick={handleLogout} className="text-slate-700">
              <LogOut size={14} aria-hidden="true" />
              <span className="hidden sm:inline">Sign out</span>
            </Button>
          </div>
        </div>
      </header>

      {/* Main Layout Container (Sidebar + Content) */}
      <div className="mx-auto flex min-h-0 w-full max-w-7xl flex-1">
        {/* Desktop Sidebar */}
        <aside className="hidden w-64 shrink-0 overflow-y-auto border-r border-slate-200 bg-white p-4 py-6 md:block">
          <nav aria-label="Admin Sidebar Navigation">{renderNavLinks()}</nav>
        </aside>

        {/* Mobile Navigation Drawer Overlay */}
        {isMobileMenuOpen && (
          <div className="fixed inset-0 z-50 md:hidden flex">
            <div
              className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs"
              onClick={() => setIsMobileMenuOpen(false)}
            />
            <div
              role="dialog"
              aria-modal="true"
              aria-label="Navigation Menu"
              className="relative flex w-full max-w-xs flex-1 flex-col bg-white p-5 shadow-xl"
            >
              <div className="flex items-center justify-between mb-6 pb-4 border-b border-slate-100">
                <div className="flex items-center gap-2">
                  <img
                    src={school.logoUrl ?? schoolLogo}
                    onError={(event) => {
                      event.currentTarget.onerror = null;
                      event.currentTarget.src = schoolLogo;
                    }}
                    alt="Logo"
                    className="size-8 rounded-full border border-slate-200"
                  />
                  <span className="font-bold text-slate-900 text-sm">Navigation Menu</span>
                </div>
                <button
                  type="button"
                  onClick={() => setIsMobileMenuOpen(false)}
                  className="p-1 text-slate-500 hover:text-slate-900"
                  aria-label="Close Navigation Menu"
                >
                  <X size={20} />
                </button>
              </div>

              <div className="flex-1 overflow-y-auto pr-1">
                {renderNavLinks(() => setIsMobileMenuOpen(false))}
              </div>

              <div className="pt-4 border-t border-slate-100 mt-4">
                <Button
                  variant="outline"
                  onClick={handleLogout}
                  className="w-full justify-center text-red-600 hover:bg-red-50 hover:border-red-200"
                >
                  <LogOut size={16} />
                  <span>Sign out</span>
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* Main Admin Content Area */}
        <main
          ref={mainContentRef}
          id="admin-main-content"
          tabIndex={-1}
          className="min-w-0 flex-1 overflow-y-auto p-4 focus:outline-none sm:p-6 lg:p-8"
        >
          <Outlet />
        </main>
      </div>
    </div>
  );
}
