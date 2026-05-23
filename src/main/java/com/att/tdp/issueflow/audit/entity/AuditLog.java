package com.att.tdp.issueflow.audit.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Append only audit row.
// beforeState and afterState are JSONB diff payloads, not full entity snapshots. Keeps the
// log compact and means the audit data is not tied to the live entity schema.
@Entity
@Table(name = "audit_logs")
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AuditActor actor;

  @Column(name = "actor_id")
  private Long actorId;

  @Column(nullable = false, length = 64)
  private String action;

  @Column(name = "entity_type", nullable = false, length = 64)
  private String entityType;

  @Column(name = "entity_id")
  private Long entityId;

  @Type(JsonBinaryType.class)
  @Column(name = "before_state", columnDefinition = "jsonb")
  private Map<String, Object> beforeState;

  @Type(JsonBinaryType.class)
  @Column(name = "after_state", columnDefinition = "jsonb")
  private Map<String, Object> afterState;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public AuditActor getActor() {
    return actor;
  }

  public void setActor(AuditActor actor) {
    this.actor = actor;
  }

  public Long getActorId() {
    return actorId;
  }

  public void setActorId(Long actorId) {
    this.actorId = actorId;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public void setEntityId(Long entityId) {
    this.entityId = entityId;
  }

  public Map<String, Object> getBeforeState() {
    return beforeState;
  }

  public void setBeforeState(Map<String, Object> beforeState) {
    this.beforeState = beforeState;
  }

  public Map<String, Object> getAfterState() {
    return afterState;
  }

  public void setAfterState(Map<String, Object> afterState) {
    this.afterState = afterState;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
