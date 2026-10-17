package com.rorfost.schoolportal.tenant.repository;

import com.rorfost.schoolportal.tenant.domain.SchoolGatewayCredential;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository interface for looking up and managing school gateway credentials. */
public interface SchoolGatewayCredentialRepository
        extends JpaRepository<SchoolGatewayCredential, UUID> {

    Optional<SchoolGatewayCredential> findByTokenDigest(byte[] tokenDigest);

    List<SchoolGatewayCredential> findAllBySchoolId(UUID schoolId);

    List<SchoolGatewayCredential> findAllBySchoolIdAndRevokedAtIsNull(UUID schoolId);

    long countBySchoolIdAndRevokedAtIsNull(UUID schoolId);

    @Query(
            """
        SELECT c FROM SchoolGatewayCredential c
        WHERE c.tokenDigest = :tokenDigest
          AND c.revokedAt IS NULL
          AND (c.expiresAt IS NULL OR c.expiresAt > :now)
    """)
    Optional<SchoolGatewayCredential> findActiveByTokenDigest(
            @Param("tokenDigest") byte[] tokenDigest, @Param("now") Instant now);
}
