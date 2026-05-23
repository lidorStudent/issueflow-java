package com.att.tdp.issueflow.common.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// The Spring Security principal we attach to the request once the JWT is verified.
// Only carries the three claims we need: user id, username and role.
// Password is unused since JWT auth does not have password material per request, and the
// UserDetails account-lockout toggles always return true.
public record UserPrincipal(Long id, String username, String role) implements UserDetails {

  // Spring expects roles to start with "ROLE_". hasRole('ADMIN') matches "ROLE_ADMIN".
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  public boolean isAdmin() {
    return "ADMIN".equals(role);
  }
}
