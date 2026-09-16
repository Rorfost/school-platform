package com.rorfost.schoolportal.dashboard.api;

public record DashboardSummaryResponse(
    long totalStudents,
    long totalStandards,
    long totalSubjects,
    long publishedMaterials,
    long publishedNotices,
    long publishedGalleryAlbums,
    long totalDownloads) {}
