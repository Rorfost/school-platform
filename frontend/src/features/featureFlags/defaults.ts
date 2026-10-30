import type { FeatureFlagsMap, FeatureKey } from "./types";

export const DEFAULT_FEATURE_FLAGS: FeatureFlagsMap = {
    GALLERY: true,
    MATERIALS: true,
    NOTICES: true,
    DOWNLOADS: true,
    EXAM_RESULTS: true,
    TIMETABLE: false,
    ATTENDANCE: false,
};

export function getDefaultFeatureEnabled(key: FeatureKey | string): boolean {
    return Boolean(DEFAULT_FEATURE_FLAGS[key]);
}
