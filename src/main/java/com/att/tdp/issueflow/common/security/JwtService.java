package com.att.tdp.issueflow.common.security;

import com.att.tdp.issueflow.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

// Issues and parses the HS256 JWTs used by /auth/login and the auth filter.
// Each token has a random jti so logout can revoke it in the deny list.
// Secret comes from ISSUEFLOW_JWT_SECRET and must be at least 256 bits.
@Service
public class JwtService {

  private final SecretKey key;
  private final JwtProperties props;
  private final Clock clock;

  public JwtService(JwtProperties props, Clock clock) {
    this.props = props;
    this.clock = clock;
    this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
  }

  // Build a signed JWT for the user. jti is a fresh UUID so we can revoke this exact token
  // at logout. username and role are baked into the token so the filter can build a
  // UserPrincipal without a DB hit on every request.
  public IssuedToken issue(Long userId, String username, String role) {
    Instant now = Instant.now(clock);
    Instant exp = now.plusSeconds(props.ttlSeconds());
    String jti = UUID.randomUUID().toString();
    String token =
        Jwts.builder()
            .id(jti)
            .issuer(props.issuer())
            .subject(String.valueOf(userId))
            .claim("username", username)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(key)
            .compact();
    return new IssuedToken(token, jti, exp, props.ttlSeconds());
  }

  // Throws if the token is unsigned, signed with the wrong key, has the wrong issuer, or
  // is expired. Callers should treat any exception here as a 401.
  public Claims parse(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .requireIssuer(props.issuer())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public boolean isValid(String token) {
    try {
      parse(token);
      return true;
    } catch (JwtException | IllegalArgumentException ex) {
      return false;
    }
  }

  public record IssuedToken(String token, String jti, Instant expiresAt, long ttlSeconds) {}
}
