package com.att.tdp.issueflow.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.config.JwtProperties;
import io.jsonwebtoken.Claims;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private final JwtProperties props =
      new JwtProperties(
          "test-secret-key-must-be-at-least-256-bits-long-for-hs256-okayyyyyyy",
          3600,
          "issueflow-test");

  @Test
  void issuesAndParsesValidToken() {
    JwtService service = new JwtService(props, Clock.systemUTC());
    var issued = service.issue(42L, "alice", "DEVELOPER");

    Claims claims = service.parse(issued.token());
    assertThat(claims.getSubject()).isEqualTo("42");
    assertThat(claims.get("username", String.class)).isEqualTo("alice");
    assertThat(claims.get("role", String.class)).isEqualTo("DEVELOPER");
    assertThat(claims.getId()).isEqualTo(issued.jti());
    assertThat(claims.getIssuer()).isEqualTo("issueflow-test");
  }

  @Test
  void rejectsTamperedToken() {
    JwtService service = new JwtService(props, Clock.systemUTC());
    var issued = service.issue(1L, "x", "ADMIN");
    String tampered = issued.token().substring(0, issued.token().length() - 4) + "xxxx";
    assertThat(service.isValid(tampered)).isFalse();
  }

  @Test
  void rejectsExpiredToken() {
    Clock past = Clock.fixed(Instant.now().minusSeconds(7200), ZoneId.of("UTC"));
    JwtService service = new JwtService(props, past);
    var issued = service.issue(1L, "x", "ADMIN");
    assertThat(service.isValid(issued.token())).isFalse();
  }
}
