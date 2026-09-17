import { useEffect } from "react";
import { Outlet, useLocation } from "react-router-dom";
import { Footer } from "@/components/layout/Footer";
import { Header } from "@/components/layout/Header";
import { SkipToContent } from "@/components/common/SkipToContent";

export function PublicLayout() {
  const location = useLocation();

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, [location.pathname]);

  return (
    <div className="flex min-h-dvh min-w-0 flex-col overflow-x-hidden bg-stone-50 text-slate-900">
      <SkipToContent targetId="main-content" />
      <Header />
      <main
        id="main-content"
        tabIndex={-1}
        className="mx-auto w-full min-w-0 max-w-6xl flex-1 px-4 py-6 sm:px-6 sm:py-10 focus:outline-none"
      >
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
