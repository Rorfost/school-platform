export const queryKeys = {
  school: ["school"] as const,
  principalProfile: ["principal-profile"] as const,
  visits: ["visits"] as const,
  standards: ["standards"] as const,
  subjects: ["subjects"] as const,
  currentAcademicYear: ["academic-years", "current"] as const,
  assessmentTypes: ["assessment-types"] as const,
  notices: (filters?: { page?: number; size?: number }) => ["notices", filters ?? {}] as const,
  materials: (filters?: { page?: number; size?: number }) => ["materials", filters ?? {}] as const,
  downloads: (filters?: { page?: number; size?: number }) => ["downloads", filters ?? {}] as const,
  galleryAlbums: (filters?: { page?: number; size?: number }) =>
    ["gallery", "albums", filters ?? {}] as const,
  galleryAlbumImages: (albumId: string) => ["gallery", "albums", albumId, "images"] as const,
  authMe: ["admin", "auth", "me"] as const,
  adminDashboardSummary: ["admin", "dashboard", "summary"] as const,
  adminAuditLogs: ["admin", "audit-logs"] as const,
  // Admin-scoped academic management keys
  adminAcademicYears: ["admin", "academic-years"] as const,
  adminStandards: ["admin", "standards"] as const,
  adminSubjects: ["admin", "subjects"] as const,
  adminAcademicSetup: ["admin", "academic-setup"] as const,
  adminStandardSubjects: (standardId: string) =>
    ["admin", "standards", standardId, "subjects"] as const,
  adminMaterials: (filters?: { page?: number; size?: number }) =>
    ["admin", "materials", filters ?? {}] as const,
  adminNotices: (filters?: { page?: number; size?: number }) =>
    ["admin", "notices", filters ?? {}] as const,
  adminDownloads: (filters?: { page?: number; size?: number }) =>
    ["admin", "downloads", filters ?? {}] as const,
  adminGalleryAlbums: (filters?: { page?: number; size?: number }) =>
    ["admin", "gallery", "albums", filters ?? {}] as const,
  adminGalleryAlbumImages: (albumId: string) =>
    ["admin", "gallery", "albums", albumId, "images"] as const,
};
