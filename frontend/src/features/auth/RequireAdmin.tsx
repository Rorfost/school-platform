import { Navigate, Outlet, useLocation } from "react-router-dom";
import { ErrorState, LoadingState } from "@/components/common/StatusPanel";
import { useAuth } from "@/features/auth/useAuth";

export function RequireAdmin() {
  const { isAuthenticated, isLoading, refetchSession, sessionError = false } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50 p-6">
        <LoadingState message="Checking administrator session..." />
      </div>
    );
  }

  if (sessionError) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50 p-6">
        <ErrorState
          message="Could not restore the administrator session. Try again."
          onRetry={() => void refetchSession()}
        />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/admin/login" state={{ from: location }} replace />;
  }

  return <Outlet />;
}
