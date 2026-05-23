package com.att.tdp.issueflow.common.security;

import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Server side logout list. We do not want signed tokens to outlive logout, so POST /auth/logout
// puts the jti here and the auth filter checks it on every request.
// We drop rows once the original token would have expired anyway, no point keeping them.
@Service
public class JwtDenyListService {

  private final JwtDenyListRepository repo;
  private final Clock clock;

  public JwtDenyListService(JwtDenyListRepository repo, Clock clock) {
    this.repo = repo;
    this.clock = clock;
  }

  @Transactional
  public void revoke(String jti, Instant expiresAt) {
    if (jti == null || repo.existsByJti(jti)) {
      return;
    }
    repo.save(new JwtDenyListEntity(jti, expiresAt, Instant.now(clock)));
  }

  public boolean isRevoked(String jti) {
    return jti != null && repo.existsByJti(jti);
  }

  // Hourly cleanup. Drop entries whose original token would have expired by now.
  @Scheduled(cron = "${issueflow.deny-list.purge-cron}")
  @Transactional
  public void purgeExpired() {
    repo.deleteExpired(Instant.now(clock));
  }
}
