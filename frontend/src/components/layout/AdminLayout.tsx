import { ExternalLink, LogOut, ShieldCheck } from "lucide-react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import schoolLogo from "@/assets/school-logo.jpeg";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/features/auth/AuthContext";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";

export function AdminLayout() {
  const { principal, logout } = useAuth();
  const school = useEffectiveSchoolInfo();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/admin/login");
  };

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col text-slate-900">
      {/* Admin Top Header */}
      <header className="border-b border-slate-200 bg-white sticky top-0 z-30">
        <div className="mx-auto flex min-h-16 max-w-7xl items-center justify-between gap-4 px-4 sm:px-6">
          <div className="flex items-center gap-3">
            <img
              src={schoolLogo}
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
              <p className="text-xs text-slate-500 font-medium hidden sm:block">
                {school.name}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <span className="hidden md:inline-block text-xs text-slate-600 font-medium">
              {principal?.email}
            </span>

            <Link
              to="/"
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1.5 text-xs font-medium text-slate-600 hover:text-slate-900 p-2 rounded-lg hover:bg-slate-50"
              title="Open public website in new tab"
            >
              <ExternalLink size={15} aria-hidden="true" />
              <span className="hidden sm:inline">View Site</span>
            </Link>

            <Button
              variant="outline"
              size="sm"
              onClick={handleLogout}
              className="text-slate-700"
            >
              <LogOut size={14} aria-hidden="true" />
              <span>Sign out</span>
            </Button>
          </div>
        </div>

        {/* Admin Navigation Strip */}
        <div className="border-t border-slate-100 bg-slate-50/70 px-4 sm:px-6">
          <nav className="mx-auto flex max-w-7xl gap-2 overflow-x-auto py-2" aria-label="Admin Navigation">
            <NavLink
              to="/admin"
              end
              className={({ isActive }) =>
                `rounded-md px-3 py-1.5 text-xs sm:text-sm font-medium transition-colors ${
                  isActive
                    ? "bg-white text-blue-900 shadow-xs border border-slate-200 font-semibold"
                    : "text-slate-600 hover:bg-white/60 hover:text-slate-900"
                }`
              }
            >
              Overview
            </NavLink>
          </nav>
        </div>
      </header>

      {/* Main Admin Workspace */}
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6 sm:px-6 sm:py-8">
        <Outlet />
      </main>
    </div>
  );
}
