package com.rorfost.schoolportal.tenant.application;

import java.util.UUID;

/**
 * Result returned directly to the operator command. The plaintext token is revealed only in this
 * in-memory object and must never be logged.
 */
public record ProvisioningResult(
        UUID schoolId,
        String slug,
        String name,
        String hostname,
        String principalEmail,
        String rawGatewayToken,
        String tokenLabel,
        UUID credentialId,
        boolean isNewSchool) {}
