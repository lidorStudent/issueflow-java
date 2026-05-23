package com.att.tdp.issueflow.ticket.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Ticket row.
// @Version handles optimistic locking. deletedAt is the soft delete tombstone.
// lastEscalatedAt is the cursor the escalation scheduler uses to avoid re-escalating.
@Entity
@Table(name = "tickets")
@EntityListeners(AuditingEntityListener.class)
public class Ticket {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(nullable = false)
  private String title;

  @Column private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private TicketStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private TicketPriority priority;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private TicketType type;

  @Column(name = "assignee_id")
  private Long assigneeId;

  @Column(name = "due_date")
  private Instant dueDate;

  @Column(name = "is_overdue", nullable = false)
  private boolean overdue;

  @Column(name = "last_escalated_at")
  private Instant lastEscalatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private Long version;

  public boolean isDeleted() {
    return deletedAt != null;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public TicketStatus getStatus() {
    return status;
  }

  public void setStatus(TicketStatus status) {
    this.status = status;
  }

  public TicketPriority getPriority() {
    return priority;
  }

  public void setPriority(TicketPriority priority) {
    this.priority = priority;
  }

  public TicketType getType() {
    return type;
  }

  public void setType(TicketType type) {
    this.type = type;
  }

  public Long getAssigneeId() {
    return assigneeId;
  }

  public void setAssigneeId(Long assigneeId) {
    this.assigneeId = assigneeId;
  }

  public Instant getDueDate() {
    return dueDate;
  }

  public void setDueDate(Instant dueDate) {
    this.dueDate = dueDate;
  }

  public boolean isOverdue() {
    return overdue;
  }

  public void setOverdue(boolean overdue) {
    this.overdue = overdue;
  }

  public Instant getLastEscalatedAt() {
    return lastEscalatedAt;
  }

  public void setLastEscalatedAt(Instant lastEscalatedAt) {
    this.lastEscalatedAt = lastEscalatedAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(Instant deletedAt) {
    this.deletedAt = deletedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
