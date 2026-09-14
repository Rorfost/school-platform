import { isRouteErrorResponse, useRouteError } from "react-router-dom";
import { ErrorState } from "@/components/common/StatusPanel";

export function RouteErrorPage() {
  const error = useRouteError();
  const message =
    isRouteErrorResponse(error) && error.status === 404 ? "પાનું મળ્યું નથી." : undefined;

  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6 sm:py-12">
      <ErrorState>{message}</ErrorState>
    </main>
  );
}
