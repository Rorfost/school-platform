package com.rorfost.schoolportal.auth;

import static com.rorfost.schoolportal.tenant.security.GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.auth.application.LoginRateLimiter;
import com.rorfost.schoolportal.school.domain.AdminUser;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.AdminUserRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.domain.SchoolGatewayCredential;
import com.rorfost.schoolportal.tenant.repository.SchoolGatewayCredentialRepository;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthenticationIntegrationTest {

    private static final String EMAIL = "principal@example.test";
    private static final String PASSWORD = "InitialPassword12";

    private static final String GATEWAY_TOKEN = "auth-integration-test-gateway-token-123456789";

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("school_portal_auth_test")
                    .withUsername("school_portal")
                    .withPassword("school_portal_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private SchoolRepository schoolRepository;

    @Autowired private AdminUserRepository adminUserRepository;

    @Autowired private SchoolGatewayCredentialRepository schoolGatewayCredentialRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private LoginRateLimiter loginRateLimiter;

    @Autowired private JdbcTemplate jdbcTemplate;

    private School activeSchool;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM admin_users");
        jdbcTemplate.update("DELETE FROM school_gateway_credentials");
        jdbcTemplate.update("DELETE FROM schools");

        loginRateLimiter.clear("127.0.0.1");

        activeSchool =
                schoolRepository.saveAndFlush(
                        new School("Authentication School", "auth-" + UUID.randomUUID()));

        schoolGatewayCredentialRepository.saveAndFlush(
                new SchoolGatewayCredential(
                        activeSchool.getId(),
                        SchoolGatewayCredential.computeDigest(GATEWAY_TOKEN),
                        "Auth Test Gateway"));
    }

    @Test
    void createsServerSideSessionForValidPrincipalLoginWithoutExposingCredentials()
            throws Exception {

        createPrincipal(true);

        MvcResult login = login(EMAIL, PASSWORD);

        Cookie sessionCookie = login.getResponse().getCookie("SESSION");

        assertThat(sessionCookie).isNotNull();
        assertThat(sessionCookie.isHttpOnly()).isTrue();
        assertThat(sessionCookie.getSecure()).isTrue();

        assertThat(login.getResponse().getContentAsString()).doesNotContain("passwordHash");

        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM spring_session", Integer.class))
                .isPositive();

        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM audit_logs WHERE action = 'LOGIN_SUCCESS'",
                                Integer.class))
                .isEqualTo(1);

        mockMvc.perform(withGatewayToken(get("/api/v1/admin/auth/me")).cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void issuesACsrfCookieAndTokenForSpaClients() throws Exception {

        createPrincipal(false);

        MvcResult csrf =
                mockMvc.perform(withGatewayToken(get("/api/v1/admin/auth/csrf")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.token").isNotEmpty())
                        .andReturn();

        Cookie sessionCookie = csrf.getResponse().getCookie("SESSION");
        Cookie csrfCookie = csrf.getResponse().getCookie("XSRF-TOKEN");

        var csrfResponse = objectMapper.readTree(csrf.getResponse().getContentAsString());

        String token = csrfResponse.path("token").asText();
        String headerName = csrfResponse.path("headerName").asText();

        assertThat(sessionCookie).isNotNull();
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.getValue()).isEqualTo(token);
        assertThat(token).isNotBlank();
        assertThat(headerName).isEqualTo("X-XSRF-TOKEN");
    }

    @Test
    void rejectsUnknownInactiveAndIncorrectCredentialsWithTheSameResponse() throws Exception {

        createPrincipal(false);

        assertAuthenticationFailure(loginRequest(EMAIL, "WrongPassword12"));
        assertAuthenticationFailure(loginRequest("unknown@example.test", PASSWORD));

        jdbcTemplate.update("UPDATE admin_users SET is_active = FALSE WHERE email = ?", EMAIL);

        assertAuthenticationFailure(loginRequest(EMAIL, PASSWORD));

        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM audit_logs WHERE action = 'LOGIN_FAILED'",
                                Integer.class))
                .isEqualTo(3);
    }

    @Test
    void validatesLoginInputAndLimitsRepeatedFailures() throws Exception {

        mockMvc.perform(
                        withGatewayToken(post("/api/v1/admin/auth/login"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        mockMvc.perform(
                        withGatewayToken(post("/api/v1/admin/auth/login"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"not-an-email\",\"password\":\"value\"}"))
                .andExpect(status().isBadRequest());

        createPrincipal(false);

        for (int attempt = 0; attempt < 5; attempt++) {
            assertAuthenticationFailure(loginRequest(EMAIL, "WrongPassword12"));
        }

        mockMvc.perform(loginRequest(EMAIL, "WrongPassword12"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("login_rate_limited"));
    }

    @Test
    void protectsAdminRoutesAndLeavesPublicRoutesOutsideAuthentication() throws Exception {

        mockMvc.perform(withGatewayToken(get("/api/v1/admin/auth/me")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));

        mockMvc.perform(withGatewayToken(get("/api/v1/admin/gallery/albums")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));

        mockMvc.perform(withGatewayToken(get("/api/v1/public/not-implemented")))
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        withGatewayToken(get("/api/v1/admin/auth/me"))
                                .cookie(new Cookie("SESSION", UUID.randomUUID().toString())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidatesTheSessionOnLogoutAndAllowsSafeRepeatedLogout() throws Exception {

        createPrincipal(false);

        MvcResult login = login(EMAIL, PASSWORD);
        Cookie sessionCookie = login.getResponse().getCookie("SESSION");

        mockMvc.perform(
                        withGatewayToken(post("/api/v1/admin/auth/logout"))
                                .cookie(sessionCookie)
                                .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(withGatewayToken(get("/api/v1/admin/auth/me")).cookie(sessionCookie))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        withGatewayToken(post("/api/v1/admin/auth/logout"))
                                .cookie(sessionCookie)
                                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void changesPasswordRotatesTheSessionAndClearsTheMandatoryChangeFlag() throws Exception {

        createPrincipal(true);

        MvcResult login = login(EMAIL, PASSWORD);
        Cookie oldSessionCookie = login.getResponse().getCookie("SESSION");

        MvcResult changed =
                mockMvc.perform(
                                withGatewayToken(put("/api/v1/admin/auth/password"))
                                        .cookie(oldSessionCookie)
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"currentPassword\":\"InitialPassword12\",\"newPassword\":\"ChangedPassword12\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.mustChangePassword").value(false))
                        .andReturn();

        Cookie newSessionCookie = changed.getResponse().getCookie("SESSION");

        assertThat(newSessionCookie).isNotNull();
        assertThat(newSessionCookie.getValue()).isNotEqualTo(oldSessionCookie.getValue());

        mockMvc.perform(withGatewayToken(get("/api/v1/admin/auth/me")).cookie(oldSessionCookie))
                .andExpect(status().isUnauthorized());

        assertAuthenticationFailure(loginRequest(EMAIL, PASSWORD));

        login(EMAIL, "ChangedPassword12");

        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM audit_logs WHERE action = 'PASSWORD_CHANGED'",
                                Integer.class))
                .isEqualTo(1);
    }

    @Test
    void rejectsIncorrectOrInvalidPasswordChangesAndRequiresCsrfForMutations() throws Exception {

        createPrincipal(false);

        MvcResult login = login(EMAIL, PASSWORD);
        Cookie sessionCookie = login.getResponse().getCookie("SESSION");

        mockMvc.perform(
                        withGatewayToken(put("/api/v1/admin/auth/password"))
                                .cookie(sessionCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"currentPassword\":\"InitialPassword12\",\"newPassword\":\"ChangedPassword12\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("csrf_invalid"));

        mockMvc.perform(
                        withGatewayToken(post("/api/v1/admin/gallery/albums"))
                                .cookie(sessionCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"School event\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("csrf_invalid"));

        mockMvc.perform(
                        withGatewayToken(put("/api/v1/admin/auth/password"))
                                .cookie(sessionCookie)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"currentPassword\":\"WrongPassword12\",\"newPassword\":\"ChangedPassword12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("password_change_rejected"));

        mockMvc.perform(
                        withGatewayToken(put("/api/v1/admin/auth/password"))
                                .cookie(sessionCookie)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"currentPassword\":\"InitialPassword12\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void appliesCredentialedCorsOnlyToConfiguredOrigin() throws Exception {

        mockMvc.perform(
                        withGatewayToken(options("/api/v1/admin/auth/login"))
                                .header("Origin", "http://localhost:5173")
                                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mockMvc.perform(
                        withGatewayToken(options("/api/v1/admin/auth/login"))
                                .header("Origin", "https://untrusted.example")
                                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    private AdminUser createPrincipal(boolean mustChangePassword) {

        AdminUser user =
                adminUserRepository.saveAndFlush(
                        new AdminUser(
                                activeSchool.getId(), EMAIL, passwordEncoder.encode(PASSWORD)));

        if (!mustChangePassword) {
            jdbcTemplate.update(
                    "UPDATE admin_users SET must_change_password = FALSE WHERE id = ?",
                    user.getId());
        }

        return user;
    }

    private MvcResult login(String email, String password) throws Exception {

        MvcResult result = mockMvc.perform(loginRequest(email, password)).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        return result;
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(
            String email, String password) throws Exception {

        return withGatewayToken(post("/api/v1/admin/auth/login"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                java.util.Map.of(
                                        "email", email,
                                        "password", password)));
    }

    private void assertAuthenticationFailure(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_failed"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            withGatewayToken(
                    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
                            request) {

        return request.header(GATEWAY_TOKEN_HEADER, GATEWAY_TOKEN);
    }
}
