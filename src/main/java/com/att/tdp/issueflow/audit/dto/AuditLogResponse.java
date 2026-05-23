package com.att.tdp.issueflow.audit.dto;

import com.att.tdp.issueflow.audit.entity.AuditActor;
import java.time.Instant;
import java.util.Map;

public record AuditLogResponse(
    Long id,
    String action,
    String entityType,
    Long entityId,
    Long performedBy,
    AuditActor actor,
    Map<String, Object> before,
    Map<String, Object> after,
    Instant timestamp) {}
