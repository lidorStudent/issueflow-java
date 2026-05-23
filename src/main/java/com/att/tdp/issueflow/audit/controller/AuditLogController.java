package com.att.tdp.issueflow.audit.controller;

import com.att.tdp.issueflow.audit.dto.AuditLogResponse;
import com.att.tdp.issueflow.audit.entity.AuditActor;
import com.att.tdp.issueflow.audit.entity.AuditLog;
import com.att.tdp.issueflow.audit.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Read only audit log endpoint.
// Filter params map directly onto JPA criteria predicates. Results are paged and newest first.
@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {

  private final AuditLogRepository repo;

  public AuditLogController(AuditLogRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public List<AuditLogResponse> list(
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) Long entityId,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) AuditActor actor,
      @RequestParam(required = false) Long actorId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {

    // Build the criteria from whatever filters the caller actually supplied. A null param
    // means "skip this predicate", not "match where the column is null".
    Specification<AuditLog> spec =
        (root, q, cb) -> {
          List<Predicate> preds = new ArrayList<>();
          if (entityType != null) preds.add(cb.equal(root.get("entityType"), entityType));
          if (entityId != null) preds.add(cb.equal(root.get("entityId"), entityId));
          if (action != null) preds.add(cb.equal(root.get("action"), action));
          if (actor != null) preds.add(cb.equal(root.get("actor"), actor));
          if (actorId != null) preds.add(cb.equal(root.get("actorId"), actorId));
          return cb.and(preds.toArray(Predicate[]::new));
        };

    Page<AuditLog> result =
        repo.findAll(
            spec,
            PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "createdAt")));

    return result.stream()
        .map(
            a ->
                new AuditLogResponse(
                    a.getId(),
                    a.getAction(),
                    a.getEntityType(),
                    a.getEntityId(),
                    a.getActorId(),
                    a.getActor(),
                    a.getBeforeState(),
                    a.getAfterState(),
                    a.getCreatedAt()))
        .toList();
  }
}
