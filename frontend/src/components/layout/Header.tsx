import { GraduationCap } from "lucide-react";
import { Link, NavLink } from "react-router-dom";

export function Header() {
  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex min-h-16 max-w-6xl items-center justify-between gap-4 px-4 sm:px-6">
        <Link
          className="flex items-center gap-3 rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700"
          to="/"
        >
          <span
            className="grid size-10 place-items-center rounded-full bg-emerald-900 text-white"
            aria-hidden="true"
          >
            <GraduationCap size={22} />
          </span>
          <span className="text-base font-semibold tracking-tight">શાળા પોર્ટલ</span>
        </Link>
        <nav aria-label="મુખ્ય માર્ગદર્શન">
          <NavLink
            className="rounded-md px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700"
            to="/"
          >
            મુખપૃષ્ઠ
          </NavLink>
        </nav>
      </div>
    </header>
  );
}
