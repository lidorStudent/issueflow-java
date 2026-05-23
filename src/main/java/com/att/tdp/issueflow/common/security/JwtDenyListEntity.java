package com.att.tdp.issueflow.common.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "jwt_deny_list")
public class JwtDenyListEntity {

  @Id
  @Column(nullable = false, length = 64)
  private String jti;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at", nullable = false)
  private Instant revokedAt;

  public JwtDenyListEntity() {}

  public JwtDenyListEntity(String jti, Instant expiresAt, Instant revokedAt) {
    this.jti = jti;
    this.expiresAt = expiresAt;
    this.revokedAt = revokedAt;
  }

  public String getJti() {
    return jti;
  }

  public void setJti(String jti) {
    this.jti = jti;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(Instant revokedAt) {
    this.revokedAt = revokedAt;
  }
}
