export const queryKeys = {
  school: ["school"] as const,
  principalProfile: ["principal-profile"] as const,
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
};
