package com.rorfost.schoolportal.assessment.repository;

import com.rorfost.schoolportal.assessment.domain.ResultPresentationSettings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResultPresentationSettingsRepository
        extends JpaRepository<ResultPresentationSettings, UUID> {}
