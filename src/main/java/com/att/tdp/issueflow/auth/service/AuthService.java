package com.att.tdp.issueflow.auth.service;

import com.att.tdp.issueflow.auth.dto.LoginRequest;
import com.att.tdp.issueflow.auth.dto.LoginResponse;
import com.att.tdp.issueflow.common.security.JwtDenyListService;
import com.att.tdp.issueflow.common.security.JwtService;
import com.att.tdp.issueflow.user.entity.User;
import com.att.tdp.issueflow.user.repository.UserRepository;
import java.time.Instant;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Checks credentials, issues JWTs and revokes them on logout.
// Login ignores soft deleted users, so a deactivated account cannot sign back in.
@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final JwtDenyListService jwtDenyListService;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      JwtDenyListService jwtDenyListService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.jwtDenyListService = jwtDenyListService;
  }

  // Check the password against bcrypt and return a fresh bearer token.
  // Same error message for "no such user" and "wrong password", so we do not leak which
  // usernames exist.
  public LoginResponse login(LoginRequest request) {
    User user =
        userRepository
            .findActiveByUsernameIgnoreCase(request.username())
            .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid username or password");
    }
    var issuedToken = jwtService.issue(user.getId(), user.getUsername(), user.getRole().name());
    return LoginResponse.bearer(issuedToken.token(), issuedToken.ttlSeconds());
  }

  public void logout(String jti, Instant expiresAt) {
    jwtDenyListService.revoke(jti, expiresAt);
  }
}
