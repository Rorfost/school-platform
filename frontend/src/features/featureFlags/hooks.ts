import { useQuery } from "@tanstack/react-query";
import { queryKeys } from "@/api/queryKeys";
import { fetchPublicFeatures } from "./api";
import { DEFAULT_FEATURE_FLAGS } from "./defaults";
import type { FeatureFlagsMap, FeatureKey } from "./types";

export function useFeatureFlags() {
    const query = useQuery<FeatureFlagsMap>({
        queryKey: queryKeys.features,
        queryFn: fetchPublicFeatures,
        staleTime: 5 * 60 * 1000, // 5 minutes cache
    });

    /**
     * Check if a feature is enabled.
     * If server response has arrived, uses exact server map.
     * If data has not yet loaded (or in headless unit tests without mocked server),
     * falls back to DEFAULT_FEATURE_FLAGS while keeping unreleased/disabled features off.
     */
    const isFeatureEnabled = (key: FeatureKey | string): boolean => {
        if (query.data) {
            return Boolean(query.data[key]);
        }
        return Boolean(DEFAULT_FEATURE_FLAGS[key]);
    };

    return {
        features: query.data ?? {},
        isLoading: query.isLoading,
        isError: query.isError,
        error: query.error,
        isFeatureEnabled,
        refetch: query.refetch,
    };
}
