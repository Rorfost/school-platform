package com.rorfost.schoolportal.assessment.repository;

import com.rorfost.schoolportal.assessment.domain.ResultPresentationSettings;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultPresentationSettingsRepository
    extends JpaRepository<ResultPresentationSettings, UUID> {}
