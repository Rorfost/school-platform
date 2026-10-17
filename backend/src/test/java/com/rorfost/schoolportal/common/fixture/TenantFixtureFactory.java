/*
 * Copyright (c) Rorfost. All rights reserved.
 * SPDX-License-Identifier: PROPRIETARY
 */

package com.rorfost.schoolportal.common.fixture;

import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.school.domain.AdminRole;
import com.rorfost.schoolportal.school.domain.PrincipalProfile;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.application.TenantResolver;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import java.util.Optional;
import java.util.UUID;

/**
 * Shared synthetic fixture factory for multi-tenant integration tests.
 *
 * <p>Provides deterministic School A, B and C objects with disjoint IDs, emails, phone numbers and
 * gateway tokens. Fixture data is purely synthetic — no production school IDs, student records or
 * real credentials are used.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * TenantFixtureFactory fixtures = new TenantFixtureFactory();
 * fixtures.stubTenantResolver(mockTenantResolver);
 * }</pre>
 */
public class TenantFixtureFactory {

    // ── Gateway tokens ──────────────────────────────────────────────────────
    public static final String TOKEN_A = "worker-secret-school-a";
    public static final String TOKEN_B = "worker-secret-school-b";
    public static final String TOKEN_C = "worker-secret-school-c";
    public static final String TOKEN_INACTIVE = "worker-secret-school-inactive";
    public static final String TOKEN_UNKNOWN = "unknown-forged-token";

    // ── Deterministic school IDs ─────────────────────────────────────────────
    public final UUID schoolIdA = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    public final UUID schoolIdB = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    public final UUID schoolIdC = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    public final UUID schoolIdInactive = UUID.fromString("dddddddd-0000-0000-0000-000000000004");

    // ── School domain objects ────────────────────────────────────────────────
    public final School schoolA;
    public final School schoolB;
    public final School schoolC;
    public final School schoolInactive;

    // ── Principal profiles ───────────────────────────────────────────────────
    public final PrincipalProfile profileA;
    public final PrincipalProfile profileB;
    public final PrincipalProfile profileC;

    // ── TenantContexts ───────────────────────────────────────────────────────
    public final TenantContext contextA;
    public final TenantContext contextB;
    public final TenantContext contextC;

    // ── Admin user IDs ───────────────────────────────────────────────────────
    public final UUID adminIdA = UUID.fromString("a0a0a0a0-0000-0000-0000-000000000001");
    public final UUID adminIdB = UUID.fromString("b0b0b0b0-0000-0000-0000-000000000002");
    public final UUID adminIdC = UUID.fromString("c0c0c0c0-0000-0000-0000-000000000003");

    // ── Admin sessions ───────────────────────────────────────────────────────
    public final PrincipalSession sessionA;
    public final PrincipalSession sessionB;
    public final PrincipalSession sessionC;

    public TenantFixtureFactory() {
        // School A — full profile, published gallery and content
        schoolA = new School("School A Primary", "school-a");
        schoolA.update(
                "School A Primary",
                "School A",
                "24010100001",
                "Address A, Village A, Sami",
                "Patan",
                "Sami",
                "Patan",
                "Gujarat",
                "384245",
                "contact@school-a.test",
                "9876543211",
                "https://school-a.example",
                null,
                "About School A",
                "1990",
                "Gujarati",
                "Primary");
        schoolA.changeLogo("schools/" + schoolIdA + "/branding/logo.png");

        // School B — different district, no logo yet
        schoolB = new School("School B Academy", "school-b");
        schoolB.update(
                "School B Academy",
                "School B",
                "24010100002",
                "Address B, Village B",
                "Radhanpur",
                "Radhanpur",
                "Patan",
                "Gujarat",
                "385340",
                "contact@school-b.test",
                "9876543222",
                "https://school-b.example",
                null,
                "About School B",
                "2005",
                "Gujarati",
                "Primary");

        // School C — third tenant for three-way isolation checks
        schoolC = new School("School C High", "school-c");
        schoolC.update(
                "School C High",
                "School C",
                "24010100003",
                "Address C, City C",
                "Mehsana",
                "Mehsana",
                "Mehsana",
                "Gujarat",
                "384001",
                "contact@school-c.test",
                "9876543333",
                "https://school-c.example",
                null,
                "About School C",
                "2010",
                "Gujarati",
                "Secondary");

        // Inactive school — deactivated
        schoolInactive = new School("Inactive School", "school-inactive");
        schoolInactive.deactivate();

        // Principal profiles
        profileA = new PrincipalProfile(schoolIdA, "Principal A Name");
        profileA.update(
                "Principal A Name",
                "Bio A",
                "M.A., B.Ed.",
                "Headmaster A",
                "Welcome from School A",
                "schools/" + schoolIdA + "/portraits/principal.jpg",
                "principal@school-a.test",
                "9876543299",
                true,
                true);

        profileB = new PrincipalProfile(schoolIdB, "Principal B Name");
        profileB.update(
                "Principal B Name",
                "Bio B",
                "B.Ed.",
                "Principal B",
                "Welcome from School B",
                null,
                "principal@school-b.test",
                "9876543288",
                true,
                false);

        profileC = new PrincipalProfile(schoolIdC, "Principal C Name");
        profileC.update(
                "Principal C Name",
                "Bio C",
                "M.Ed.",
                "Director C",
                "Welcome from School C",
                null,
                "principal@school-c.test",
                "9876543277",
                true,
                true);

        // Tenant contexts
        contextA = new TenantContext(schoolIdA, "school-a");
        contextB = new TenantContext(schoolIdB, "school-b");
        contextC = new TenantContext(schoolIdC, "school-c");

        // Admin sessions (school ownership is baked into the session)
        sessionA =
                new PrincipalSession(
                        adminIdA, schoolIdA, "principal@school-a.test", AdminRole.PRINCIPAL, false);
        sessionB =
                new PrincipalSession(
                        adminIdB, schoolIdB, "principal@school-b.test", AdminRole.PRINCIPAL, false);
        sessionC =
                new PrincipalSession(
                        adminIdC, schoolIdC, "principal@school-c.test", AdminRole.PRINCIPAL, false);
    }

    /**
     * Registers gateway-token → TenantContext stubs on a Mockito-mocked {@link TenantResolver}.
     *
     * @param tenantResolver a Mockito mock of TenantResolver
     */
    public void stubTenantResolver(TenantResolver tenantResolver) {
        when(tenantResolver.resolveTenant(TOKEN_A)).thenReturn(Optional.of(contextA));
        when(tenantResolver.resolveTenant(TOKEN_B)).thenReturn(Optional.of(contextB));
        when(tenantResolver.resolveTenant(TOKEN_C)).thenReturn(Optional.of(contextC));
        when(tenantResolver.resolveTenant(TOKEN_INACTIVE)).thenReturn(Optional.empty());
        when(tenantResolver.resolveTenant(TOKEN_UNKNOWN)).thenReturn(Optional.empty());
    }

    /**
     * Returns a MockMvc request post-processor that injects School A's TenantContext directly into
     * the request attributes, bypassing the gateway filter (for use in admin controller tests).
     */
    public static org.springframework.test.web.servlet.request.RequestPostProcessor withContextA(
            TenantContext context) {
        return request -> {
            TenantContextHolder.setTenantContext(request, context);
            return request;
        };
    }
}
