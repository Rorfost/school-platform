import { useEffect } from "react";
import { Outlet, useLocation, NavLink } from "react-router-dom";
import { Footer } from "@/components/layout/Footer";
import { Header, NAV_LINKS } from "@/components/layout/Header";
import { SkipToContent } from "@/components/common/SkipToContent";
import { BookOpen, Calculator, GraduationCap } from "lucide-react";
import { LABELS } from "@/utils/gujarati";

function PublicSidebar() {
  return (
    <nav className="flex flex-col gap-1" aria-label="મુખ્ય માર્ગદર્શન">
      {NAV_LINKS.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          className={({ isActive }) =>
            `flex min-h-10 items-center rounded-lg px-3.5 py-2 text-sm font-medium transition-colors ${
              isActive
                ? "bg-blue-50 text-blue-900 font-semibold"
                : "text-slate-700 hover:bg-slate-100 hover:text-slate-900"
            }`
          }
        >
          {item.label}
        </NavLink>
      ))}

      <div className="mt-6 border-t border-slate-200 pt-5">
        <p className="px-3.5 text-xs font-semibold text-slate-500 uppercase tracking-wider">
          {LABELS.studentCorner}
        </p>
        <div className="mt-2 flex flex-col gap-1">
          <NavLink
            to="/student/materials"
            className="flex min-h-10 items-center gap-3 rounded-lg px-3.5 text-sm text-slate-700 hover:bg-slate-100"
          >
            <BookOpen size={16} className="text-slate-500" />
            <span>{LABELS.materials}</span>
          </NavLink>
          <NavLink
            to="/tools"
            className="flex min-h-10 items-center gap-3 rounded-lg px-3.5 text-sm text-slate-700 hover:bg-slate-100"
          >
            <Calculator size={16} className="text-slate-500" />
            <span>{LABELS.tools}</span>
          </NavLink>
          <NavLink
            to="/student/results"
            className="flex min-h-10 items-center gap-3 rounded-lg px-3.5 text-sm text-slate-700 hover:bg-slate-100"
          >
            <GraduationCap size={16} className="text-slate-500" />
            <span>{LABELS.results}</span>
          </NavLink>
        </div>
      </div>
    </nav>
  );
}

export function PublicLayout() {
  const location = useLocation();

  useEffect(() => {
    const mainContent = document.getElementById("main-content");
    if (mainContent) {
      mainContent.scrollTo({ top: 0, behavior: "auto" });
    }
  }, [location.pathname]);

  return (
    <div className="flex h-dvh flex-col overflow-hidden bg-stone-50 text-slate-900">
      <SkipToContent targetId="main-content" />
      <Header />
      <div className="flex min-h-0 w-full flex-1 mx-auto max-w-7xl">
        {/* Desktop Sidebar */}
        <aside className="hidden w-64 shrink-0 overflow-y-auto border-r border-slate-200 bg-white p-4 py-6 lg:block">
          <PublicSidebar />
        </aside>

        {/* Main Content Area */}
        <main
          id="main-content"
          tabIndex={-1}
          className="min-w-0 flex-1 overflow-y-auto focus:outline-none flex flex-col"
        >
          <div className="flex-1 px-4 py-6 sm:px-6 sm:py-10">
            <Outlet />
          </div>
          <Footer />
        </main>
      </div>
    </div>
  );
}
