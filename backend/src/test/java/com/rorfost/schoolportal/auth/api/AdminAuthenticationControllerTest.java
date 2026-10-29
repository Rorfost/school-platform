package com.rorfost.schoolportal.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.auth.application.AdminAuthenticationService;
import com.rorfost.schoolportal.auth.application.AuthenticationFailedException;
import com.rorfost.schoolportal.auth.application.LoginRateLimiter;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.school.domain.AdminRole;
import com.rorfost.schoolportal.security.turnstile.TurnstileVerificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.DeferredCsrfToken;

import java.util.UUID;

class AdminAuthenticationControllerTest {

    private AdminAuthenticationService authenticationService;
    private LoginRateLimiter loginRateLimiter;
    private CsrfTokenRepository csrfTokenRepository;
    private TurnstileVerificationService turnstileVerificationService;
    private AdminAuthenticationController controller;

    private PrincipalSession principal;
    private UUID adminUserId;
    private UUID schoolId;

    @BeforeEach
    void setUp() {
        authenticationService = Mockito.mock(AdminAuthenticationService.class);
        loginRateLimiter = Mockito.mock(LoginRateLimiter.class);
        csrfTokenRepository = Mockito.mock(CsrfTokenRepository.class);
        turnstileVerificationService = Mockito.mock(TurnstileVerificationService.class);

        controller =
                new AdminAuthenticationController(
                        authenticationService,
                        loginRateLimiter,
                        csrfTokenRepository,
                        turnstileVerificationService);

        adminUserId = UUID.randomUUID();
        schoolId = UUID.randomUUID();
        principal =
                new PrincipalSession(
                        adminUserId, schoolId, "principal@example.com", AdminRole.PRINCIPAL, false);
    }

    @Test
    void csrf_shouldReturnCsrfTokenAndHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "test-token-value");

        DeferredCsrfToken deferredCsrfToken = Mockito.mock(DeferredCsrfToken.class);
        when(deferredCsrfToken.get()).thenReturn(token);
        when(csrfTokenRepository.loadDeferredToken(request, response))
                .thenReturn(deferredCsrfToken);

        CsrfTokenResponse result = controller.csrf(request, response);

        assertThat(result.token()).isEqualTo("test-token-value");
        assertThat(result.headerName()).isEqualTo("X-XSRF-TOKEN");
    }

    @Test
    void login_shouldAuthenticateAndClearRateLimiter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1"); // internal proxy address
        // Trusted client IP injected by the Cloudflare Worker
        request.addHeader(AdminAuthenticationController.CLIENT_IP_HEADER, "203.0.113.55");
        com.rorfost.schoolportal.tenant.application.TenantContextHolder.setTenantContext(
                request,
                new com.rorfost.schoolportal.tenant.domain.TenantContext(schoolId, "school-slug"));

        when(authenticationService.authenticate(
                        schoolId, "principal@example.com", "Password12345!"))
                .thenReturn(principal);

        LoginRequest loginRequest = new LoginRequest("principal@example.com", "Password12345!");

        PrincipalAccountResponse response = controller.login(loginRequest, request);

        // Rate-limiter key must use the trusted X-Portal-Client-IP, not the proxy's remoteAddr
        String expectedKey = schoolId + ":203.0.113.55:principal@example.com";
        assertThat(response.email()).isEqualTo("principal@example.com");
        verify(loginRateLimiter).checkAllowed(expectedKey);
        verify(loginRateLimiter).clear(expectedKey);
        assertThat(request.getSession(false)).isNotNull();
    }

    @Test
    void login_fallsBackToRemoteAddrWhenClientIpHeaderAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        // No X-Portal-Client-IP header — local dev / direct call
        com.rorfost.schoolportal.tenant.application.TenantContextHolder.setTenantContext(
                request,
                new com.rorfost.schoolportal.tenant.domain.TenantContext(schoolId, "school-slug"));

        when(authenticationService.authenticate(
                        schoolId, "principal@example.com", "Password12345!"))
                .thenReturn(principal);

        LoginRequest loginRequest = new LoginRequest("principal@example.com", "Password12345!");
        controller.login(loginRequest, request);

        String expectedKey = schoolId + ":127.0.0.1:principal@example.com";
        verify(loginRateLimiter).checkAllowed(expectedKey);
    }

    @Test
    void login_fallsBackToRemoteAddrWhenClientIpHeaderIsInvalidFormat() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        // Attacker tries to inject a non-IP string through the header
        request.addHeader(AdminAuthenticationController.CLIENT_IP_HEADER, "evil-header-value!!!");
        com.rorfost.schoolportal.tenant.application.TenantContextHolder.setTenantContext(
                request,
                new com.rorfost.schoolportal.tenant.domain.TenantContext(schoolId, "school-slug"));

        when(authenticationService.authenticate(
                        schoolId, "principal@example.com", "Password12345!"))
                .thenReturn(principal);

        LoginRequest loginRequest = new LoginRequest("principal@example.com", "Password12345!");
        controller.login(loginRequest, request);

        // Falls back to remoteAddr because header format is invalid
        String expectedKey = schoolId + ":127.0.0.1:principal@example.com";
        verify(loginRateLimiter).checkAllowed(expectedKey);
    }

    @Test
    void login_onFailure_shouldRecordFailureInRateLimiter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader(AdminAuthenticationController.CLIENT_IP_HEADER, "198.51.100.10");
        com.rorfost.schoolportal.tenant.application.TenantContextHolder.setTenantContext(
                request,
                new com.rorfost.schoolportal.tenant.domain.TenantContext(schoolId, "school-slug"));

        when(authenticationService.authenticate(schoolId, "wrong@example.com", "badpass"))
                .thenThrow(new AuthenticationFailedException());

        LoginRequest loginRequest = new LoginRequest("wrong@example.com", "badpass");

        assertThatThrownBy(() -> controller.login(loginRequest, request))
                .isInstanceOf(AuthenticationFailedException.class);

        String expectedKey = schoolId + ":198.51.100.10:wrong@example.com";
        verify(loginRateLimiter).recordFailure(expectedKey);
    }

    @Test
    void login_missingTenantContext_throwsForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        LoginRequest loginRequest = new LoginRequest("principal@example.com", "Password12345!");

        assertThatThrownBy(() -> controller.login(loginRequest, request))
                .isInstanceOf(com.rorfost.schoolportal.common.exception.DomainException.class)
                .satisfies(
                        ex ->
                                assertThat(
                                                ((com.rorfost.schoolportal.common.exception
                                                                        .DomainException)
                                                                ex)
                                                        .getCode())
                                        .isEqualTo("missing_tenant_context"));
    }

    @Test
    void logout_shouldInvalidateSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);

        var response = controller.logout(request);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void me_shouldReturnCurrentPrincipalDetails() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());

        PrincipalAccountResponse response = controller.me(auth);

        assertThat(response.email()).isEqualTo("principal@example.com");
        assertThat(response.role()).isEqualTo("PRINCIPAL");
        assertThat(response.mustChangePassword()).isFalse();
    }
}
