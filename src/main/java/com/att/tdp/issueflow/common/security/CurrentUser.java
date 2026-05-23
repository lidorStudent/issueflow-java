package com.att.tdp.issueflow.common.security;

import com.att.tdp.issueflow.common.error.UnauthorizedException;
import org.springframework.security.core.context.SecurityContextHolder;

// Static getter for the current principal. Saves us from passing the user through every
// service method. Throws UnauthorizedException (401) if there is no authenticated user.
public final class CurrentUser {

  private CurrentUser() {}

  public static UserPrincipal getOrThrow() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
      throw new UnauthorizedException("UNAUTHENTICATED", "Authentication required");
    }
    return principal;
  }
}
