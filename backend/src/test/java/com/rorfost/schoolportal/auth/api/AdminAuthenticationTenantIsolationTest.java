package com.rorfost.schoolportal.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.auth.application.AdminAuthenticationService;
import com.rorfost.schoolportal.auth.application.AuthenticationFailedException;
import com.rorfost.schoolportal.auth.application.LoginRateLimiter;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.common.exception.ApiExceptionHandler;
import com.rorfost.schoolportal.school.domain.AdminRole;
import com.rorfost.schoolportal.security.turnstile.TurnstileVerificationService;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

class AdminAuthenticationTenantIsolationTest {

    private MockMvc mockMvc;
    private AdminAuthenticationService authenticationService;
    private LoginRateLimiter loginRateLimiter;
    private CsrfTokenRepository csrfTokenRepository;
    private ObjectMapper objectMapper;

    private UUID schoolAId;
    private UUID schoolBId;
    private PrincipalSession sessionSchoolA;
    private PrincipalSession sessionSchoolB;

    @BeforeEach
    void setUp() {
        authenticationService = Mockito.mock(AdminAuthenticationService.class);
        loginRateLimiter = Mockito.mock(LoginRateLimiter.class);
        csrfTokenRepository = Mockito.mock(CsrfTokenRepository.class);
        objectMapper = new ObjectMapper();

        TurnstileVerificationService turnstileVerificationService =
                Mockito.mock(TurnstileVerificationService.class);

        AdminAuthenticationController controller =
                new AdminAuthenticationController(
                        authenticationService,
                        loginRateLimiter,
                        csrfTokenRepository,
                        turnstileVerificationService);

        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new ApiExceptionHandler())
                        .build();

        schoolAId = UUID.randomUUID();
        schoolBId = UUID.randomUUID();

        sessionSchoolA =
                new PrincipalSession(
                        UUID.randomUUID(),
                        schoolAId,
                        "principal@shared.edu",
                        AdminRole.PRINCIPAL,
                        false);
        sessionSchoolB =
                new PrincipalSession(
                        UUID.randomUUID(),
                        schoolBId,
                        "principal@shared.edu",
                        AdminRole.PRINCIPAL,
                        false);
    }

    @Test
    @DisplayName("Principal from School A authenticates successfully on School A portal")
    void login_schoolA_success() throws Exception {
        when(authenticationService.authenticate(
                        schoolAId, "principal@shared.edu", "PasswordA12345!"))
                .thenReturn(sessionSchoolA);

        LoginRequest request = new LoginRequest("principal@shared.edu", "PasswordA12345!");

        mockMvc.perform(
                        post("/api/v1/admin/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(
                                        req -> {
                                            TenantContextHolder.setTenantContext(
                                                    req, new TenantContext(schoolAId, "school-a"));
                                            return req;
                                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("principal@shared.edu"))
                .andExpect(jsonPath("$.role").value("PRINCIPAL"))
                .andExpect(jsonPath("$.mustChangePassword").value(false));

        verify(authenticationService)
                .authenticate(schoolAId, "principal@shared.edu", "PasswordA12345!");
    }

    @Test
    @DisplayName("Identical email login on School B portal authenticates against School B scope")
    void login_schoolB_sameEmail_authenticatedAgainstSchoolB() throws Exception {
        when(authenticationService.authenticate(
                        schoolBId, "principal@shared.edu", "PasswordB12345!"))
                .thenReturn(sessionSchoolB);

        LoginRequest request = new LoginRequest("principal@shared.edu", "PasswordB12345!");

        mockMvc.perform(
                        post("/api/v1/admin/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(
                                        req -> {
                                            TenantContextHolder.setTenantContext(
                                                    req, new TenantContext(schoolBId, "school-b"));
                                            return req;
                                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("principal@shared.edu"))
                .andExpect(jsonPath("$.role").value("PRINCIPAL"))
                .andExpect(jsonPath("$.mustChangePassword").value(false));

        verify(authenticationService)
                .authenticate(schoolBId, "principal@shared.edu", "PasswordB12345!");
    }

    @Test
    @DisplayName(
            "Wrong school portal login fails with 401 and does not authenticate across tenants")
    void login_wrongPortal_rejectedWith401() throws Exception {
        when(authenticationService.authenticate(
                        schoolBId, "principal@shared.edu", "PasswordA12345!"))
                .thenThrow(new AuthenticationFailedException());

        LoginRequest request = new LoginRequest("principal@shared.edu", "PasswordA12345!");

        mockMvc.perform(
                        post("/api/v1/admin/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(
                                        req -> {
                                            TenantContextHolder.setTenantContext(
                                                    req, new TenantContext(schoolBId, "school-b"));
                                            return req;
                                        }))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_failed"));

        verify(authenticationService)
                .authenticate(schoolBId, "principal@shared.edu", "PasswordA12345!");
        verify(authenticationService, never()).authenticate(eq(schoolAId), any(), any());
    }

    @Test
    @DisplayName(
            "Request without verified TenantContext fails closed with 403 missing_tenant_context")
    void login_missingTenantContext_failsClosedWith403() throws Exception {
        LoginRequest request = new LoginRequest("principal@shared.edu", "PasswordA12345!");

        mockMvc.perform(
                        post("/api/v1/admin/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("missing_tenant_context"));

        verify(authenticationService, never()).authenticate(any(), any(), any());
    }
}
