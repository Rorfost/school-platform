package com.rorfost.schoolportal.content.repository;

import java.util.UUID;

/**
 * Projection carrying a study material's denormalized standard and subject display labels.
 *
 * <p>Used by {@code ContentService.materialWithLabels} to replace the 3-query-per-material pattern
 * (standardSubject lookup + standard lookup + subject lookup) with a single JPQL join. Fields are
 * nullable: a material not linked to a standard/subject returns {@code null} for both labels.
 *
 * <p>Only the display strings leave the service layer — the raw UUIDs are still available on the
 * {@code StudyMaterial} entity when needed for write paths.
 */
public record StudyMaterialLabelRow(UUID materialId, String standardName, String subjectName) {}
