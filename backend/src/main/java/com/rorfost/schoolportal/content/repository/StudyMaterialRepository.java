package com.rorfost.schoolportal.content.repository;

import com.rorfost.schoolportal.content.domain.PublicationStatus;
import com.rorfost.schoolportal.content.domain.StudyMaterial;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, UUID> {
    Page<StudyMaterial> findBySchoolIdAndStatus(
            UUID schoolId, PublicationStatus status, Pageable pageable);

    Page<StudyMaterial> findBySchoolId(UUID schoolId, Pageable pageable);

    Page<StudyMaterial> findBySchoolIdAndStatusAndAcademicYearIdAndStandardSubjectId(
            UUID schoolId,
            PublicationStatus status,
            UUID academicYearId,
            UUID standardSubjectId,
            Pageable pageable);

    java.util.Optional<StudyMaterial> findByIdAndSchoolId(UUID id, UUID schoolId);

    long countBySchoolIdAndStatus(UUID schoolId, PublicationStatus status);

    /**
     * Batch join query that resolves standard and subject display labels for a set of study
     * materials in a single SQL round-trip.
     *
     * <p>Replaces the 3-query-per-material pattern (standardSubject + standard + subject lookups)
     * that previously fired inside {@code ContentService.material()} when building list responses.
     * For a page of 20 materials this eliminates up to 60 extra queries.
     *
     * <p>The join is LEFT OUTER on {@code StandardSubject} so that materials without a {@code
     * standardSubjectId} still return a row (with {@code null} labels). The inner joins to {@code
     * Standard} and {@code Subject} are also LEFT OUTER for the same reason.
     *
     * <p>All join predicates include {@code schoolId} so a foreign school's standard/subject cannot
     * be returned even if UUIDs collide.
     */
    @Query(
            """
            select new com.rorfost.schoolportal.content.repository.StudyMaterialLabelRow(
                m.id,
                st.displayName,
                sub.name
            )
            from StudyMaterial m
            left join StandardSubject ss
                on ss.id = m.standardSubjectId
               and ss.schoolId = :schoolId
            left join Standard st
                on st.id = ss.standardId
               and st.schoolId = :schoolId
            left join Subject sub
                on sub.id = ss.subjectId
               and sub.schoolId = :schoolId
            where m.id in :materialIds
              and m.schoolId = :schoolId
            """)
    List<StudyMaterialLabelRow> findLabelsBySchoolIdAndMaterialIds(
            @Param("schoolId") UUID schoolId, @Param("materialIds") Collection<UUID> materialIds);
}
