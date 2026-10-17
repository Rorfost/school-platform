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
    private AdminAuthenticationController controller;

    private PrincipalSession principal;
    private UUID adminUserId;
    private UUID schoolId;

    @BeforeEach
    void setUp() {
        authenticationService = Mockito.mock(AdminAuthenticationService.class);
        loginRateLimiter = Mockito.mock(LoginRateLimiter.class);
        csrfTokenRepository = Mockito.mock(CsrfTokenRepository.class);

        controller =
                new AdminAuthenticationController(
                        authenticationService, loginRateLimiter, csrfTokenRepository);

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
        request.setRemoteAddr("127.0.0.1");

        when(authenticationService.authenticate("principal@example.com", "Password12345!"))
                .thenReturn(principal);

        LoginRequest loginRequest = new LoginRequest("principal@example.com", "Password12345!");

        PrincipalAccountResponse response = controller.login(loginRequest, request);

        assertThat(response.email()).isEqualTo("principal@example.com");
        verify(loginRateLimiter).checkAllowed("127.0.0.1");
        verify(loginRateLimiter).clear("127.0.0.1");
        assertThat(request.getSession(false)).isNotNull();
    }

    @Test
    void login_onFailure_shouldRecordFailureInRateLimiter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        when(authenticationService.authenticate("wrong@example.com", "badpass"))
                .thenThrow(new AuthenticationFailedException());

        LoginRequest loginRequest = new LoginRequest("wrong@example.com", "badpass");

        assertThatThrownBy(() -> controller.login(loginRequest, request))
                .isInstanceOf(AuthenticationFailedException.class);

        verify(loginRateLimiter).recordFailure("127.0.0.1");
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
