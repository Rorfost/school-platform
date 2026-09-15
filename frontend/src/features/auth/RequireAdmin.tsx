import { Navigate, Outlet, useLocation } from "react-router-dom";
import { LoadingState } from "@/components/common/StatusPanel";
import { useAuth } from "@/features/auth/useAuth";

export function RequireAdmin() {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50 p-6">
        <LoadingState message="Checking administrator session..." />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/admin/login" state={{ from: location }} replace />;
  }

  return <Outlet />;
}
