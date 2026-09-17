package com.rorfost.schoolportal.auth.api;

import com.rorfost.schoolportal.auth.application.AdminAuthenticationService;
import com.rorfost.schoolportal.auth.application.AuthenticationFailedException;
import com.rorfost.schoolportal.auth.application.LoginRateLimiter;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthenticationController {

  private final AdminAuthenticationService authenticationService;
  private final LoginRateLimiter loginRateLimiter;
  private final CsrfTokenRepository csrfTokenRepository;

  public AdminAuthenticationController(
      AdminAuthenticationService authenticationService,
      LoginRateLimiter loginRateLimiter,
      CsrfTokenRepository csrfTokenRepository) {
    this.authenticationService = authenticationService;
    this.loginRateLimiter = loginRateLimiter;
    this.csrfTokenRepository = csrfTokenRepository;
  }

  @GetMapping("/csrf")
  public CsrfTokenResponse csrf(HttpServletRequest request, HttpServletResponse response) {
    CsrfToken token = csrfTokenRepository.loadDeferredToken(request, response).get();
    return new CsrfTokenResponse(token.getToken(), token.getHeaderName());
  }

  @PostMapping("/login")
  public PrincipalAccountResponse login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
    String clientKey = servletRequest.getRemoteAddr();
    loginRateLimiter.checkAllowed(clientKey);
    try {
      PrincipalSession principal =
          authenticationService.authenticate(request.email(), request.password());
      loginRateLimiter.clear(clientKey);
      saveAuthentication(servletRequest, principal);
      return PrincipalAccountResponse.from(principal);
    } catch (AuthenticationFailedException exception) {
      loginRateLimiter.recordFailure(clientKey);
      throw exception;
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    SecurityContextHolder.clearContext();
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public PrincipalAccountResponse me(Authentication authentication) {
    return PrincipalAccountResponse.from((PrincipalSession) authentication.getPrincipal());
  }

  @PutMapping("/password")
  public PrincipalAccountResponse changePassword(
      @Valid @RequestBody PasswordChangeRequest request,
      Authentication authentication,
      HttpServletRequest servletRequest) {
    PrincipalSession currentPrincipal = (PrincipalSession) authentication.getPrincipal();
    PrincipalSession updatedPrincipal =
        authenticationService.changePassword(
            currentPrincipal, request.currentPassword(), request.newPassword());
    saveAuthentication(servletRequest, updatedPrincipal);
    return PrincipalAccountResponse.from(updatedPrincipal);
  }

  private void saveAuthentication(HttpServletRequest request, PrincipalSession principal) {
    HttpSession session = request.getSession(true);
    request.changeSessionId();
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    session.setAttribute("SPRING_SECURITY_CONTEXT", context);
  }
}
