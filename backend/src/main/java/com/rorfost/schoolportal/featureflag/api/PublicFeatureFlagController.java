package com.rorfost.schoolportal.featureflag.api;

import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.featureflag.application.FeatureFlagService;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/features")
public class PublicFeatureFlagController {

    private final FeatureFlagService featureFlagService;

    public PublicFeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @GetMapping
    public Map<String, Boolean> getPublicFeatures() {
        UUID schoolId = resolveTenantSchoolId();
        return featureFlagService.getFeaturesForSchool(schoolId);
    }

    private UUID resolveTenantSchoolId() {
        return TenantContextHolder.getCurrentTenant()
                .map(TenantContext::schoolId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "school_not_found"));
    }
}
