package com.att.tdp.issueflow.auth.controller;

import com.att.tdp.issueflow.auth.dto.LoginRequest;
import com.att.tdp.issueflow.auth.dto.LoginResponse;
import com.att.tdp.issueflow.auth.service.AuthService;
import com.att.tdp.issueflow.common.security.CurrentUser;
import com.att.tdp.issueflow.common.security.JwtService;
import com.att.tdp.issueflow.user.dto.UserResponse;
import com.att.tdp.issueflow.user.service.UserMapper;
import com.att.tdp.issueflow.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The three auth endpoints. login returns a JWT. logout revokes it via the deny list.
// /me returns the current user profile.
@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;
  private final JwtService jwtService;
  private final UserService userService;

  public AuthController(AuthService authService, JwtService jwtService, UserService userService) {
    this.authService = authService;
    this.jwtService = jwtService;
    this.userService = userService;
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  // Revoke the bearer token by adding its jti to the deny list.
  // A malformed or expired token is treated as a no-op, those cannot be reused anyway.
  @PostMapping("/logout")
  public void logout(HttpServletRequest request) {
    String authorizationHeader = request.getHeader("Authorization");
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      return;
    }
    String token = authorizationHeader.substring("Bearer ".length()).trim();
    try {
      Claims claims = jwtService.parse(token);
      authService.logout(claims.getId(), Instant.ofEpochMilli(claims.getExpiration().getTime()));
    } catch (Exception ignored) {
      // unparseable or expired tokens are effectively already invalid
    }
  }

  @GetMapping("/me")
  public UserResponse me() {
    var principal = CurrentUser.getOrThrow();
    return UserMapper.toDto(userService.findById(principal.id()));
  }
}
