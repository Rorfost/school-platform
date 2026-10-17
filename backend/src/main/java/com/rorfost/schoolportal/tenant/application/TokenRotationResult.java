package com.rorfost.schoolportal.tenant.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Result returned directly to the operator command upon token rotation. The raw replacement token
 * is revealed only in this in-memory object and must never be logged.
 */
public record TokenRotationResult(
        UUID schoolId,
        UUID oldCredentialId,
        UUID newCredentialId,
        String rawNewGatewayToken,
        String newLabel,
        Instant oldExpiresAt) {}
