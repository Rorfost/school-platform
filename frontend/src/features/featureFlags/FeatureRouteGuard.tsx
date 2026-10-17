import { ReactNode } from "react";
import { Navigate, Outlet } from "react-router-dom";
import { LoadingState } from "@/components/common/StatusPanel";
import { useFeatureFlags } from "@/features/featureFlags";
import type { FeatureKey } from "@/features/featureFlags";

interface FeatureRouteGuardProps {
    featureKey: FeatureKey;
    children?: ReactNode;
    fallbackTo?: string;
}

/**
 * Route guard that validates feature flag status before rendering children or an Outlet.
 * While flags are loading, renders a clean loading placeholder to prevent flash of content.
 * When disabled or failed, redirects to fallbackTo (default: "/404") with replace.
 */
export function FeatureRouteGuard({
    featureKey,
    children,
    fallbackTo = "/404",
}: FeatureRouteGuardProps) {
    const { isFeatureEnabled, isLoading } = useFeatureFlags();

    if (isLoading) {
        return (
            <div className="p-4 sm:p-6">
                <LoadingState message="વિગત તપાસી રહ્યા છીએ..." />
            </div>
        );
    }

    if (!isFeatureEnabled(featureKey)) {
        return <Navigate to={fallbackTo} replace />;
    }

    return children ? <>{children}</> : <Outlet />;
}
