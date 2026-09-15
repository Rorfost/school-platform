package com.rorfost.schoolportal.auth.domain;

import com.rorfost.schoolportal.school.domain.AdminRole;
import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record PrincipalSession(
    UUID adminUserId, UUID schoolId, String email, AdminRole role, boolean mustChangePassword)
    implements Principal, Serializable {

  public Collection<? extends GrantedAuthority> authorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getName() {
    return email;
  }
}
