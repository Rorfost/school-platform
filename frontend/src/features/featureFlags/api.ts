import { apiRequest } from "@/api/client";
import type { FeatureFlagsMap } from "./types";

export async function fetchPublicFeatures(): Promise<FeatureFlagsMap> {
    return apiRequest<FeatureFlagsMap>("/api/v1/public/features");
}
