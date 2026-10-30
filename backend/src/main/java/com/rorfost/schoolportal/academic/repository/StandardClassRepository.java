package com.rorfost.schoolportal.academic.repository;

import com.rorfost.schoolportal.academic.domain.StandardClass;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StandardClassRepository extends JpaRepository<StandardClass, UUID> {

    List<StandardClass> findBySchoolIdAndStandardIdOrderBySortOrder(UUID schoolId, UUID standardId);

    List<StandardClass> findBySchoolIdOrderBySortOrder(UUID schoolId);

    void deleteBySchoolIdAndStandardId(UUID schoolId, UUID standardId);

    boolean existsByStandardIdAndNameIgnoreCase(UUID standardId, String name);
}
