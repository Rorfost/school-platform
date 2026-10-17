package com.rorfost.schoolportal.tenant.application;

import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.domain.SchoolGatewayCredential;
import com.rorfost.schoolportal.tenant.domain.TenantContext;
import com.rorfost.schoolportal.tenant.repository.SchoolGatewayCredentialRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;

/**
 * Service responsible for resolving an active {@link TenantContext} from a cryptographic gateway
 * token.
 *
 * <p>Validates token digest using constant-time comparison, queries active non-revoked credentials,
 * and ensures the mapped school is active. Never writes raw tokens to logs or telemetry.
 */
@Service
public class TenantResolver {

    private static final Logger log = LoggerFactory.getLogger(TenantResolver.class);

    private final SchoolGatewayCredentialRepository credentialRepository;
    private final SchoolRepository schoolRepository;

    public TenantResolver(
            SchoolGatewayCredentialRepository credentialRepository,
            SchoolRepository schoolRepository) {
        this.credentialRepository = credentialRepository;
        this.schoolRepository = schoolRepository;
    }

    /**
     * Resolves the verified tenant context corresponding to the provided raw token string.
     *
     * @param rawToken the opaque gateway token from the {@code X-Portal-Gateway-Token} header
     * @return an {@link Optional} containing the verified {@link TenantContext}, or empty if
     *     invalid
     */
    @Transactional(readOnly = true)
    public Optional<TenantContext> resolveTenant(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        byte[] incomingDigest;
        try {
            incomingDigest = SchoolGatewayCredential.computeDigest(rawToken.strip());
        } catch (Exception e) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        Optional<SchoolGatewayCredential> maybeCredential =
                credentialRepository.findActiveByTokenDigest(incomingDigest, now);

        if (maybeCredential.isEmpty()) {
            log.warn(
                    "Gateway credential verification failed: no active credential matching token digest");
            return Optional.empty();
        }

        SchoolGatewayCredential credential = maybeCredential.get();

        // Constant-time comparison on digest bytes
        if (!MessageDigest.isEqual(incomingDigest, credential.getTokenDigest())) {
            return Optional.empty();
        }

        Optional<School> maybeSchool = schoolRepository.findById(credential.getSchoolId());
        if (maybeSchool.isEmpty()) {
            log.warn(
                    "Gateway credential mapped to non-existent schoolId={}",
                    credential.getSchoolId());
            return Optional.empty();
        }

        School school = maybeSchool.get();
        if (!school.isActive()) {
            log.warn("Gateway credential mapped to inactive schoolId={}", school.getId());
            return Optional.empty();
        }

        return Optional.of(new TenantContext(school.getId(), school.getSlug()));
    }
}
