package com.att.tdp.issueflow.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Reads Authorization: Bearer ..., validates the JWT and puts a UserPrincipal in the
// SecurityContext. Bad or revoked tokens leave the context empty so the rest of the chain
// returns 401 or 403 as needed.
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final String BEARER = "Bearer ";

  private final JwtService jwtService;
  private final JwtDenyListService denyList;

  public JwtAuthFilter(JwtService jwtService, JwtDenyListService denyList) {
    this.jwtService = jwtService;
    this.denyList = denyList;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith(BEARER)) {
      chain.doFilter(request, response);
      return;
    }

    String token = header.substring(BEARER.length()).trim();
    try {
      Claims claims = jwtService.parse(token);
      // Revocation check. A logged out token is still a valid signature until it expires,
      // the deny list is what makes it stop working immediately.
      String jti = claims.getId();
      if (denyList.isRevoked(jti)) {
        SecurityContextHolder.clearContext();
        chain.doFilter(request, response);
        return;
      }

      Long userId = Long.valueOf(claims.getSubject());
      String username = claims.get("username", String.class);
      String role = claims.get("role", String.class);

      UserPrincipal principal = new UserPrincipal(userId, username, role);
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
      auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(auth);
    } catch (JwtException | IllegalArgumentException ignored) {
      SecurityContextHolder.clearContext();
    }

    chain.doFilter(request, response);
  }
}
