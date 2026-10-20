package com.rorfost.schoolportal.tenant.application;

import com.rorfost.schoolportal.audit.domain.AuditAction;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.school.domain.AdminUser;
import com.rorfost.schoolportal.school.domain.PrincipalProfile;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.AdminUserRepository;
import com.rorfost.schoolportal.school.repository.PrincipalProfileRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.domain.SchoolDomain;
import com.rorfost.schoolportal.tenant.domain.SchoolGatewayCredential;
import com.rorfost.schoolportal.tenant.repository.SchoolDomainRepository;
import com.rorfost.schoolportal.tenant.repository.SchoolGatewayCredentialRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Service providing operator-controlled workflows for school provisioning, gateway credential
 * rotation/revocation, and school lifecycle management.
 *
 * <p>Never exposed to public or principal-authenticated HTTP endpoints. Raw gateway tokens are
 * returned strictly in-memory once to the operator and never logged.
 */
@Service
public class TenantProvisioningService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int GATEWAY_TOKEN_BYTE_LENGTH = 32; // 256 bits

    private final SchoolRepository schoolRepository;
    private final SchoolDomainRepository schoolDomainRepository;
    private final SchoolGatewayCredentialRepository credentialRepository;
    private final AdminUserRepository adminUserRepository;
    private final PrincipalProfileRepository principalProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public TenantProvisioningService(
            SchoolRepository schoolRepository,
            SchoolDomainRepository schoolDomainRepository,
            SchoolGatewayCredentialRepository credentialRepository,
            AdminUserRepository adminUserRepository,
            PrincipalProfileRepository principalProfileRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {
        this.schoolRepository = schoolRepository;
        this.schoolDomainRepository = schoolDomainRepository;
        this.credentialRepository = credentialRepository;
        this.adminUserRepository = adminUserRepository;
        this.principalProfileRepository = principalProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    /**
     * Provisions a new school or idempotently ensures an existing school's initial configuration.
     * Generates a 256-bit cryptographically secure gateway token and returns it once in the result.
     */
    @Transactional
    public ProvisioningResult provisionSchool(
            String slug,
            String displayName,
            String allowedHostname,
            String principalEmail,
            String initialPassword) {
        Objects.requireNonNull(slug, "slug must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(principalEmail, "principalEmail must not be null");

        String normalizedSlug = slug.strip().toLowerCase(Locale.ROOT);
        String normalizedHostname = SchoolDomain.normalizeHostname(allowedHostname);
        String normalizedEmail = principalEmail.strip().toLowerCase(Locale.ROOT);

        // Check hostname uniqueness across schools
        Optional<SchoolDomain> existingDomain =
                schoolDomainRepository.findByHostname(normalizedHostname);

        Optional<School> existingSchool = schoolRepository.findBySlug(normalizedSlug);
        School school;
        boolean isNewSchool = false;

        if (existingSchool.isPresent()) {
            school = existingSchool.get();
            // Validate domain binding consistency
            if (existingDomain.isPresent()
                    && !existingDomain.get().getSchoolId().equals(school.getId())) {
                throw new DomainException(
                        HttpStatus.CONFLICT, "hostname_already_bound_to_different_school");
            }
        } else {
            if (existingDomain.isPresent()) {
                throw new DomainException(
                        HttpStatus.CONFLICT, "hostname_already_bound_to_different_school");
            }
            school = schoolRepository.save(new School(displayName.strip(), normalizedSlug));
            isNewSchool = true;

            // Create initial shell for principal profile
            principalProfileRepository.save(
                    new PrincipalProfile(school.getId(), displayName.strip() + " Principal"));
        }

        // Bind or ensure domain
        if (existingDomain.isEmpty()) {
            schoolDomainRepository.save(new SchoolDomain(school.getId(), normalizedHostname, true));
        }

        // Provision principal user if not present
        if (adminUserRepository.findBySchoolIdAndEmail(school.getId(), normalizedEmail).isEmpty()) {
            if (initialPassword == null || initialPassword.isBlank()) {
                throw new DomainException(
                        HttpStatus.BAD_REQUEST, "initial_password_required_for_principal");
            }
            String encodedPassword = passwordEncoder.encode(initialPassword.strip());
            AdminUser admin = new AdminUser(school.getId(), normalizedEmail, encodedPassword);
            adminUserRepository.save(admin);
        }

        // Idempotent gateway credential handling:
        // If the school already has an active gateway credential, do not create a new one during
        // repeat provisioning.
        List<SchoolGatewayCredential> activeCredentials =
                credentialRepository.findActiveBySchoolId(school.getId(), Instant.now());

        String rawToken;
        String label;
        UUID credentialId;

        if (activeCredentials.isEmpty()) {
            rawToken = generateSecureGatewayToken();
            byte[] digest = SchoolGatewayCredential.computeDigest(rawToken);
            label = "gateway-token-" + normalizedSlug + "-" + System.currentTimeMillis();

            SchoolGatewayCredential credential =
                    credentialRepository.save(
                            new SchoolGatewayCredential(school.getId(), digest, label));
            credentialId = credential.getId();
        } else {
            // Existing active credential retained; token was already issued and its raw value
            // cannot be retrieved.
            SchoolGatewayCredential existingCred = activeCredentials.get(0);
            rawToken = null;
            label = existingCred.getLabel();
            credentialId = existingCred.getId();
        }

        auditLogService.record(
                school.getId(),
                null,
                AuditAction.TENANT_PROVISIONED,
                "SCHOOL",
                school.getId(),
                "OPERATOR_CLI");

        return new ProvisioningResult(
                school.getId(),
                school.getSlug(),
                school.getName(),
                normalizedHostname,
                normalizedEmail,
                rawToken,
                label,
                credentialId,
                isNewSchool);
    }

    /**
     * Rotates a gateway token for a school with a two-token overlap window. The old token is given
     * a grace period expiration, and the new token is active immediately.
     */
    @Transactional
    public TokenRotationResult rotateGatewayCredential(
            UUID schoolId, UUID oldCredentialId, Duration overlapGracePeriod) {
        School school =
                schoolRepository
                        .findById(schoolId)
                        .orElseThrow(
                                () ->
                                        new DomainException(
                                                HttpStatus.NOT_FOUND, "school_not_found"));

        SchoolGatewayCredential oldCredential =
                credentialRepository
                        .findById(oldCredentialId)
                        .filter(c -> c.getSchoolId().equals(schoolId))
                        .orElseThrow(
                                () ->
                                        new DomainException(
                                                HttpStatus.NOT_FOUND,
                                                "gateway_credential_not_found"));

        if (!oldCredential.isActive()) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST, "gateway_credential_already_inactive");
        }

        Instant now = Instant.now();
        Instant oldExpiresAt =
                now.plus(overlapGracePeriod != null ? overlapGracePeriod : Duration.ofHours(24));

        oldCredential.recordRotation(now);
        oldCredential.setExpiresAt(oldExpiresAt);
        credentialRepository.save(oldCredential);

        String rawNewToken = generateSecureGatewayToken();
        byte[] newDigest = SchoolGatewayCredential.computeDigest(rawNewToken);
        String newLabel = "gateway-token-" + school.getSlug() + "-" + System.currentTimeMillis();

        SchoolGatewayCredential newCredential =
                credentialRepository.save(
                        new SchoolGatewayCredential(school.getId(), newDigest, newLabel));

        auditLogService.record(
                school.getId(),
                null,
                AuditAction.GATEWAY_TOKEN_ROTATED,
                "GATEWAY_CREDENTIAL",
                newCredential.getId(),
                "OPERATOR_CLI");

        return new TokenRotationResult(
                school.getId(),
                oldCredential.getId(),
                newCredential.getId(),
                rawNewToken,
                newLabel,
                oldExpiresAt);
    }

    /** Immediately revokes a gateway credential. */
    @Transactional
    public void revokeGatewayCredential(UUID schoolId, UUID credentialId) {
        SchoolGatewayCredential credential =
                credentialRepository
                        .findById(credentialId)
                        .filter(c -> c.getSchoolId().equals(schoolId))
                        .orElseThrow(
                                () ->
                                        new DomainException(
                                                HttpStatus.NOT_FOUND,
                                                "gateway_credential_not_found"));

        credential.revoke(Instant.now());
        credentialRepository.save(credential);

        auditLogService.record(
                schoolId,
                null,
                AuditAction.GATEWAY_TOKEN_REVOKED,
                "GATEWAY_CREDENTIAL",
                credentialId,
                "OPERATOR_CLI");
    }

    /** Activates a school tenant. */
    @Transactional
    public void activateSchool(UUID schoolId) {
        School school =
                schoolRepository
                        .findById(schoolId)
                        .orElseThrow(
                                () ->
                                        new DomainException(
                                                HttpStatus.NOT_FOUND, "school_not_found"));
        school.activate();
        schoolRepository.save(school);

        auditLogService.record(
                school.getId(),
                null,
                AuditAction.TENANT_ACTIVATED,
                "SCHOOL",
                school.getId(),
                "OPERATOR_CLI");
    }

    /** Deactivates a school tenant, making all gateway lookups immediately fail closed. */
    @Transactional
    public void deactivateSchool(UUID schoolId) {
        School school =
                schoolRepository
                        .findById(schoolId)
                        .orElseThrow(
                                () ->
                                        new DomainException(
                                                HttpStatus.NOT_FOUND, "school_not_found"));
        school.deactivate();
        schoolRepository.save(school);

        auditLogService.record(
                school.getId(),
                null,
                AuditAction.TENANT_DEACTIVATED,
                "SCHOOL",
                school.getId(),
                "OPERATOR_CLI");
    }

    private String generateSecureGatewayToken() {
        byte[] randomBytes = new byte[GATEWAY_TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
