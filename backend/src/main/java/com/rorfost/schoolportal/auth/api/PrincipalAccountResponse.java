package com.rorfost.schoolportal.auth.api;

import com.rorfost.schoolportal.auth.domain.PrincipalSession;

public record PrincipalAccountResponse(String email, String role, boolean mustChangePassword) {

  static PrincipalAccountResponse from(PrincipalSession principal) {
    return new PrincipalAccountResponse(
        principal.email(), principal.role().name(), principal.mustChangePassword());
  }
}
