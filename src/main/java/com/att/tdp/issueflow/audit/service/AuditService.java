package com.att.tdp.issueflow.audit.service;

import com.att.tdp.issueflow.audit.entity.AuditActor;
import com.att.tdp.issueflow.audit.entity.AuditLog;
import com.att.tdp.issueflow.audit.repository.AuditLogRepository;
import com.att.tdp.issueflow.common.audit.AuditContext;
import com.att.tdp.issueflow.common.security.UserPrincipal;
import java.util.Map;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Writes append only audit rows. Every state changing service calls in here.
// Actor resolution: explicit SYSTEM override wins, otherwise the authenticated user,
// otherwise we fall back to SYSTEM.
@Service
public class AuditService {

  private final AuditLogRepository repo;

  public AuditService(AuditLogRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public void recordCreate(String entityType, Long entityId, Map<String, Object> after) {
    write("CREATE", entityType, entityId, null, after);
  }

  @Transactional
  public void recordUpdate(
      String entityType, Long entityId, Map<String, Object> before, Map<String, Object> after) {
    write("UPDATE", entityType, entityId, before, after);
  }

  @Transactional
  public void recordDelete(String entityType, Long entityId, Map<String, Object> before) {
    write("DELETE", entityType, entityId, before, null);
  }

  @Transactional
  public void recordCustom(
      String action,
      String entityType,
      Long entityId,
      Map<String, Object> before,
      Map<String, Object> after) {
    write(action, entityType, entityId, before, after);
  }

  private void write(
      String action,
      String entityType,
      Long entityId,
      Map<String, Object> before,
      Map<String, Object> after) {
    AuditLog log = new AuditLog();
    log.setAction(action);
    log.setEntityType(entityType);
    log.setEntityId(entityId);
    log.setBeforeState(before);
    log.setAfterState(after);

    // Actor priority: explicit override (background work), then the logged in user, then SYSTEM.
    String override = AuditContext.currentActorOverride();
    if ("SYSTEM".equals(override)) {
      log.setActor(AuditActor.SYSTEM);
      log.setActorId(null);
    } else {
      var auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
        log.setActor(AuditActor.USER);
        log.setActorId(principal.id());
      } else {
        log.setActor(AuditActor.SYSTEM);
        log.setActorId(null);
      }
    }
    repo.save(log);
  }
}
