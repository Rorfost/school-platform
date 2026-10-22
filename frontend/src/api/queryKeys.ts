export interface PageFilters {
    page?: number;
    size?: number;
}

export type ResultType = "ANNUAL" | "EKAM_KASOTI";

/**
 * Creates cache keys for one portal origin. The origin partitions browser caches only; it is
 * never sent as, or trusted for, backend tenant authorization.
 */
export function createQueryKeys(portalOrigin: string) {
    const portalRoot = ["portal", portalOrigin] as const;
    const publicRoot = [...portalRoot, "public"] as const;
    const adminRoot = [...portalRoot, "admin"] as const;

    return {
        portalRoot,
        publicRoot,
        adminRoot,
        school: [...publicRoot, "school"] as const,
        principalProfile: [...publicRoot, "principal-profile"] as const,
        features: [...publicRoot, "features"] as const,
        visits: [...publicRoot, "visits"] as const,
        standards: [...publicRoot, "standards"] as const,
        subjects: [...publicRoot, "subjects"] as const,
        assessmentTypes: [...publicRoot, "assessment-types"] as const,
        resultSettingsRoot: [...publicRoot, "result-settings"] as const,
        resultSettings: (standard: string, resultType: ResultType) =>
            [...publicRoot, "result-settings", standard, resultType] as const,
        noticesRoot: [...publicRoot, "notices"] as const,
        notices: (filters?: PageFilters) => [...publicRoot, "notices", filters ?? {}] as const,
        notice: (noticeId: string) => [...publicRoot, "notices", "detail", noticeId] as const,
        materialsRoot: [...publicRoot, "materials"] as const,
        materials: (filters?: PageFilters) => [...publicRoot, "materials", filters ?? {}] as const,
        downloadsRoot: [...publicRoot, "downloads"] as const,
        downloads: (filters?: PageFilters) => [...publicRoot, "downloads", filters ?? {}] as const,
        galleryAlbumsRoot: [...publicRoot, "gallery", "albums"] as const,
        galleryAlbums: (filters?: PageFilters) =>
            [...publicRoot, "gallery", "albums", filters ?? {}] as const,
        galleryAlbumImages: (albumId: string, filters?: PageFilters) =>
            [...publicRoot, "gallery", "albums", albumId, "images", filters ?? {}] as const,
        authMe: [...adminRoot, "auth", "me"] as const,
        adminSchool: [...adminRoot, "school"] as const,
        adminPrincipalProfile: [...adminRoot, "principal-profile"] as const,
        adminDashboardSummary: [...adminRoot, "dashboard", "summary"] as const,
        adminAuditLogs: [...adminRoot, "audit-logs"] as const,
        adminAcademicYears: [...adminRoot, "academic-years"] as const,
        adminStandards: [...adminRoot, "standards"] as const,
        adminSubjects: [...adminRoot, "subjects"] as const,
        adminAcademicSetup: [...adminRoot, "academic-setup"] as const,
        adminStandardSubjects: (standardId: string) =>
            [...adminRoot, "standards", standardId, "subjects"] as const,
        adminResultSettingsRoot: [...adminRoot, "result-settings"] as const,
        adminResultSettings: (resultType: ResultType) =>
            [...adminRoot, "result-settings", resultType] as const,
        adminMaterialsRoot: [...adminRoot, "materials"] as const,
        adminMaterials: (filters?: PageFilters) =>
            [...adminRoot, "materials", filters ?? {}] as const,
        adminNoticesRoot: [...adminRoot, "notices"] as const,
        adminNotices: (filters?: PageFilters) => [...adminRoot, "notices", filters ?? {}] as const,
        adminDownloadsRoot: [...adminRoot, "downloads"] as const,
        adminDownloads: (filters?: PageFilters) =>
            [...adminRoot, "downloads", filters ?? {}] as const,
        adminGalleryAlbumsRoot: [...adminRoot, "gallery", "albums"] as const,
        adminGalleryAlbums: (filters?: PageFilters) =>
            [...adminRoot, "gallery", "albums", filters ?? {}] as const,
        adminGalleryAlbumImages: (albumId: string) =>
            [...adminRoot, "gallery", "albums", albumId, "images"] as const,
    };
}

function getPortalOrigin(): string {
    return typeof window === "undefined" ? "server" : window.location.origin;
}

export const queryKeys = createQueryKeys(getPortalOrigin());
