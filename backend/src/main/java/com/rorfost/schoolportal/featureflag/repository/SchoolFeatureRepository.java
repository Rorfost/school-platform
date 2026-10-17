package com.rorfost.schoolportal.featureflag.repository;

import com.rorfost.schoolportal.featureflag.domain.SchoolFeature;
import com.rorfost.schoolportal.featureflag.domain.SchoolFeatureId;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolFeatureRepository extends JpaRepository<SchoolFeature, SchoolFeatureId> {

    List<SchoolFeature> findByIdSchoolId(UUID schoolId);

    Optional<SchoolFeature> findByIdSchoolIdAndIdFeatureKey(UUID schoolId, String featureKey);
}
