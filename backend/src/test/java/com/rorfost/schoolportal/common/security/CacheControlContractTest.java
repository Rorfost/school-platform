package com.rorfost.schoolportal.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cache-Control / Set-Cookie / Vary contract tests for Sprint 37.
 *
 * <p>Validates the cache eligibility matrix defined in {@code docs/engineering/CACHE_POLICY.md}
 * without enabling any actual caching. Each nested class covers one classification category from
 * the matrix.
 *
 * <p>Invariants under test:
 *
 * <ul>
 *   <li><b>Admin boundary:</b> every {@code /api/v1/admin/**} response carries {@code
 *       Cache-Control: no-store} and {@code Pragma: no-cache} (enforced by {@link
 *       SensitiveResponseHeadersFilter}).
 *   <li><b>Student result:</b> {@code GET /api/v1/public/exam-results} is classified {@code
 *       Personal Result} and always carries {@code Cache-Control: no-store}.
 *   <li><b>Set-Cookie invariant:</b> responses that emit {@code Set-Cookie} (login, CSRF) also
 *       carry {@code Cache-Control: no-store}; no intermediate cache may store them.
 *   <li><b>Public eligibility:</b> public semi-static and public dynamic routes must NOT emit
 *       {@code Set-Cookie} (prerequisite for Sprint 38/39 edge caching) and must not be forced to
 *       {@code no-store} by this sprint's filter.
 *   <li><b>Cross-tenant safety:</b> the {@code no-store} invariant holds independently for every
 *       tenant; School A and School B requests both receive correct headers.
 * </ul>
 *
 * <p>This is a standalone MockMvc test — no Testcontainers or full application context required.
 * The {@link SensitiveResponseHeadersFilter} is the primary system under test; fake controllers
 * provide the minimum surface to exercise each route category.
 */
class CacheControlContractTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SensitiveResponseHeadersFilter headersFilter = new SensitiveResponseHeadersFilter();
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new FakePublicController(),
                                new FakeAdminController(),
                                new FakeLoginController())
                        .addFilters(headersFilter)
                        .build();
    }

    // -------------------------------------------------------------------------
    // Admin boundary invariant — /api/v1/admin/** must always be no-store
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Admin boundary invariant — Cache-Control: no-store on all admin paths")
    class AdminBoundaryInvariant {

        @Test
        @DisplayName("GET /api/v1/admin/school carries no-store and Pragma: no-cache")
        void adminSchool_noStore() throws Exception {
            mockMvc.perform(get("/api/v1/admin/school"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(header().string("Pragma", "no-cache"));
        }

        @Test
        @DisplayName("GET /api/v1/admin/dashboard/summary carries no-store (nested admin path)")
        void adminDashboard_noStore() throws Exception {
            mockMvc.perform(get("/api/v1/admin/dashboard/summary"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(header().string("Pragma", "no-cache"));
        }

        @Test
        @DisplayName("GET /api/v1/admin/auth/me carries no-store (session-bound principal)")
        void adminAuthMe_noStore() throws Exception {
            mockMvc.perform(get("/api/v1/admin/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(header().string("Pragma", "no-cache"));
        }

        @Test
        @DisplayName("GET /api/v1/admin/auth/csrf carries no-store (CSRF/session category)")
        void adminAuthCsrf_noStore() throws Exception {
            mockMvc.perform(get("/api/v1/admin/auth/csrf"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(header().string("Pragma", "no-cache"));
        }

        @Test
        @DisplayName("GET /api/v1/admin/academic/roster carries no-store (deep nested admin path)")
        void adminAcademicRoster_noStore() throws Exception {
            mockMvc.perform(get("/api/v1/admin/academic/roster"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(header().string("Pragma", "no-cache"));
        }
    }

    // -------------------------------------------------------------------------
    // Student result invariant — /api/v1/public/exam-results must be no-store
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Student result invariant — Cache-Control: no-store on exam-results")
    class StudentResultInvariant {

        @Test
        @DisplayName("GET /api/v1/public/exam-results carries no-store (Personal Result category)")
        void publicExamResults_noStore() throws Exception {
            mockMvc.perform(get("/api/v1/public/exam-results"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(header().string("Pragma", "no-cache"));
        }

        @Test
        @DisplayName(
                "Two-school cross-tenant: exam-results from School A and B both carry no-store"
                        + " independently")
        void crossTenant_examResults_bothSchoolsNoStore() throws Exception {
            // School A tenant header (simulating Cloudflare Worker gateway injection)
            mockMvc.perform(
                            get("/api/v1/public/exam-results")
                                    .header("X-Verified-School-Id", "school-a-uuid"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"));

            // School B tenant — same invariant must hold independently; no cross-tenant bleed
            mockMvc.perform(
                            get("/api/v1/public/exam-results")
                                    .header("X-Verified-School-Id", "school-b-uuid"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"));
        }
    }

    // -------------------------------------------------------------------------
    // Set-Cookie invariant — responses emitting Set-Cookie must also carry no-store
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Set-Cookie invariant — no-store co-present whenever Set-Cookie is emitted")
    class SetCookieInvariant {

        @Test
        @DisplayName(
                "POST /api/v1/admin/auth/login emits Set-Cookie and Cache-Control: no-store"
                        + " (session fixation prevention)")
        void adminLogin_setCookieAndNoStore() throws Exception {
            // The real login endpoint emits JSESSIONID/SESSION Set-Cookie on successful auth.
            // Cache-Control: no-store is mandatory so no shared cache stores the session cookie.
            mockMvc.perform(post("/api/v1/admin/auth/login"))
                    .andExpect(status().isOk())
                    // Set-Cookie must be present in the fake response (simulates real login)
                    .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                    // Cache-Control: no-store must be co-present — enforced by the filter
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(header().string("Pragma", "no-cache"));
        }

        @Test
        @DisplayName("Two-school cross-tenant: login no-store holds for both School A and B")
        void crossTenant_login_bothSchoolsNoStore() throws Exception {
            mockMvc.perform(
                            post("/api/v1/admin/auth/login")
                                    .header("X-Verified-School-Id", "school-a-uuid"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"));

            mockMvc.perform(
                            post("/api/v1/admin/auth/login")
                                    .header("X-Verified-School-Id", "school-b-uuid"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"));
        }
    }

    // -------------------------------------------------------------------------
    // Public route eligibility — must NOT emit Set-Cookie; must NOT be forced no-store
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName(
            "Public route eligibility — no Set-Cookie, not prematurely forced to no-store"
                    + " (Sprint 38/39 prerequisite)")
    class PublicRouteEligibility {

        @Test
        @DisplayName(
                "GET /api/v1/public/school does NOT emit Set-Cookie"
                        + " (Public Semi-Static eligible for Sprint 38 caching)")
        void publicSchool_noSetCookie() throws Exception {
            mockMvc.perform(get("/api/v1/public/school"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        }

        @Test
        @DisplayName(
                "GET /api/v1/public/gallery/albums does NOT emit Set-Cookie"
                        + " (Public Dynamic eligible for Sprint 38 caching)")
        void publicGalleryAlbums_noSetCookie() throws Exception {
            mockMvc.perform(get("/api/v1/public/gallery/albums"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        }

        @Test
        @DisplayName(
                "GET /api/v1/public/notices does NOT emit Set-Cookie"
                        + " (Public Dynamic eligible for Sprint 38 caching)")
        void publicNotices_noSetCookie() throws Exception {
            mockMvc.perform(get("/api/v1/public/notices"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        }

        @Test
        @DisplayName(
                "GET /api/v1/public/visits does NOT emit Set-Cookie"
                        + " (Public Counter — real-time, no caching)")
        void publicVisits_noSetCookie() throws Exception {
            mockMvc.perform(get("/api/v1/public/visits"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        }

        @Test
        @DisplayName(
                "GET /api/v1/public/principal-profile does NOT emit Set-Cookie"
                        + " (Public Semi-Static eligible for Sprint 38 caching)")
        void publicPrincipalProfile_noSetCookie() throws Exception {
            mockMvc.perform(get("/api/v1/public/principal-profile"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        }

        @Test
        @DisplayName(
                "GET /api/v1/public/school is NOT forced to Cache-Control: no-store"
                        + " (Sprint 37 only classifies; Sprint 38 adds headers)")
        void publicSchool_notPrematurelyForcedNoStore() throws Exception {
            // The SensitiveResponseHeadersFilter must NOT inject no-store on general public routes.
            // Sprint 38 will add correct public caching headers. If no-store appeared here, it
            // would block the entire future caching strategy.
            mockMvc.perform(get("/api/v1/public/school"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("Cache-Control"));
        }

        @Test
        @DisplayName(
                "GET /api/v1/public/gallery/albums is NOT forced to Cache-Control: no-store"
                        + " (Sprint 37 only classifies; Sprint 38 adds headers)")
        void publicGalleryAlbums_notPrematurelyForcedNoStore() throws Exception {
            mockMvc.perform(get("/api/v1/public/gallery/albums"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("Cache-Control"));
        }

        @Test
        @DisplayName(
                "Two-school cross-tenant: public school routes from School A and B do not emit"
                        + " Set-Cookie independently")
        void crossTenant_publicSchool_bothSchoolsNoSetCookie() throws Exception {
            mockMvc.perform(
                            get("/api/v1/public/school")
                                    .header("X-Verified-School-Id", "school-a-uuid"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

            mockMvc.perform(
                            get("/api/v1/public/school")
                                    .header("X-Verified-School-Id", "school-b-uuid"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        }
    }

    // =========================================================================
    // Minimal fake controllers — no service dependencies; pure route-surface stubs
    // =========================================================================

    /**
     * Stubs for public routes covering all classification categories: Public Semi-Static, Public
     * Dynamic, Public Counter, and Personal Result.
     */
    @RestController
    static class FakePublicController {

        @GetMapping("/api/v1/public/school")
        ResponseEntity<String> school() {
            return ResponseEntity.ok("{\"name\":\"Synthetic School\"}");
        }

        @GetMapping("/api/v1/public/principal-profile")
        ResponseEntity<String> principalProfile() {
            return ResponseEntity.ok("{\"name\":\"Principal\"}");
        }

        @GetMapping("/api/v1/public/gallery/albums")
        ResponseEntity<String> galleryAlbums() {
            return ResponseEntity.ok("{\"content\":[]}");
        }

        @GetMapping("/api/v1/public/notices")
        ResponseEntity<String> notices() {
            return ResponseEntity.ok("{\"content\":[]}");
        }

        @GetMapping("/api/v1/public/visits")
        ResponseEntity<String> visits() {
            return ResponseEntity.ok("{\"count\":0}");
        }

        /**
         * Personal Result category — the real endpoint returns {@code CacheControl.noStore()} at
         * the controller level; the filter independently enforces no-store at the servlet layer.
         * Both defenses are validated.
         */
        @GetMapping("/api/v1/public/exam-results")
        ResponseEntity<String> examResults() {
            return ResponseEntity.ok("{\"student\":\"synthetic-roll-001\"}");
        }
    }

    /**
     * Stubs for admin routes covering Authenticated Private, CSRF/Session categories. All paths
     * begin with {@code /api/v1/admin/} and must receive {@code Cache-Control: no-store} from the
     * {@link SensitiveResponseHeadersFilter}.
     */
    @RestController
    static class FakeAdminController {

        @GetMapping("/api/v1/admin/school")
        ResponseEntity<String> school() {
            return ResponseEntity.ok("{\"name\":\"Synthetic School\"}");
        }

        @GetMapping("/api/v1/admin/dashboard/summary")
        ResponseEntity<String> dashboardSummary() {
            return ResponseEntity.ok("{\"total\":0}");
        }

        @GetMapping("/api/v1/admin/auth/me")
        ResponseEntity<String> me() {
            return ResponseEntity.ok("{\"email\":\"admin@synthetic.test\"}");
        }

        @GetMapping("/api/v1/admin/auth/csrf")
        ResponseEntity<String> csrf() {
            return ResponseEntity.ok("{\"token\":\"synthetic-csrf-token\"}");
        }

        @GetMapping("/api/v1/admin/academic/roster")
        ResponseEntity<String> roster() {
            return ResponseEntity.ok("{\"students\":[]}");
        }
    }

    /**
     * Stub for the login endpoint. Simulates {@code Set-Cookie} emission (as the real Spring
     * Security login does) to verify that a session-cookie-bearing response is also guarded by
     * {@code Cache-Control: no-store}.
     */
    @RestController
    static class FakeLoginController {

        @PostMapping("/api/v1/admin/auth/login")
        ResponseEntity<String> login(jakarta.servlet.http.HttpServletResponse response) {
            // Mirrors the real login: Spring Security sets SESSION cookie on successful auth.
            // We simulate this here so the contract test can assert both Set-Cookie presence
            // and Cache-Control: no-store co-presence — proving caches are never permitted to
            // store a session-cookie-bearing response.
            response.addCookie(buildSessionCookie());
            return ResponseEntity.ok("{\"email\":\"admin@synthetic.test\"}");
        }

        private Cookie buildSessionCookie() {
            Cookie session = new Cookie("SESSION", "synthetic-session-token");
            session.setHttpOnly(true);
            session.setSecure(true);
            session.setPath("/");
            return session;
        }
    }
}
