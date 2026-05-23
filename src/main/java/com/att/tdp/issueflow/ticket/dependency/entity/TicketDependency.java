package com.att.tdp.issueflow.ticket.dependency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

// Edge row in the blocker graph. Means "ticket X is blocked by ticket Y".
// The composite key TicketDependencyId holds both endpoints, and the DB has a CHECK
// constraint that the two are different.
@Entity
@Table(name = "ticket_dependencies")
public class TicketDependency {

  @EmbeddedId private TicketDependencyId id;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public TicketDependency() {}

  public TicketDependency(Long ticketId, Long blockerId, Instant createdAt) {
    this.id = new TicketDependencyId(ticketId, blockerId);
    this.createdAt = createdAt;
  }

  public TicketDependencyId getId() {
    return id;
  }

  public void setId(TicketDependencyId id) {
    this.id = id;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
