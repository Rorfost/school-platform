import { ArrowLeft } from "lucide-react";
import { isRouteErrorResponse, Link, useRouteError } from "react-router-dom";
import { ErrorState } from "@/components/common/StatusPanel";
import { LABELS } from "@/utils/gujarati";

export function RouteErrorPage() {
  const error = useRouteError();
  const isNotFound = isRouteErrorResponse(error) && error.status === 404;

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-12 sm:px-6 sm:py-16">
      <ErrorState
        message={
          isNotFound
            ? "આ પાનું મળી શક્યું નથી."
            : "હમણાં સિસ્ટમમાં તકનીકી ખામી આવી છે. કૃપા કરીને થોડા સમય પછી પ્રયત્ન કરો."
        }
      />
      <div className="mt-6 text-center">
        <Link
          to="/"
          className="inline-flex items-center gap-2 text-sm font-semibold text-blue-900 hover:text-blue-950"
        >
          <ArrowLeft size={16} aria-hidden="true" />
          <span>{LABELS.home} પર પાછા જાઓ</span>
        </Link>
      </div>
    </main>
  );
}
