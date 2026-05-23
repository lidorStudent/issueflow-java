package com.att.tdp.issueflow.common.security;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JwtDenyListRepository extends JpaRepository<JwtDenyListEntity, String> {

  boolean existsByJti(String jti);

  @Modifying
  @Query("DELETE FROM JwtDenyListEntity j WHERE j.expiresAt < :now")
  int deleteExpired(@Param("now") Instant now);
}
