package com.rorfost.schoolportal.featureflag.api;

import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.featureflag.application.FeatureFlagService;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/features")
public class PublicFeatureFlagController {
    private static final CacheControl FEATURE_FLAG_CACHE =
            CacheControl.maxAge(Duration.ofSeconds(180))
                    .cachePublic()
                    .sMaxAge(Duration.ofSeconds(180));

    private final FeatureFlagService featureFlagService;

    public PublicFeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Boolean>> getPublicFeatures(WebRequest webRequest) {
        UUID schoolId = resolveTenantSchoolId();
        Map<String, Boolean> features = featureFlagService.getFeaturesForSchool(schoolId);
        String etag = "\"" + schoolId + ":" + features.hashCode() + "\"";
        if (webRequest != null && webRequest.checkNotModified(etag)) {
            return null;
        }
        return ResponseEntity.ok().cacheControl(FEATURE_FLAG_CACHE).eTag(etag).body(features);
    }

    private UUID resolveTenantSchoolId() {
        return TenantContextHolder.getCurrentTenant()
                .map(TenantContext::schoolId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "school_not_found"));
    }
}
