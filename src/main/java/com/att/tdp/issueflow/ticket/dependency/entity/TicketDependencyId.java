package com.att.tdp.issueflow.ticket.dependency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TicketDependencyId implements Serializable {

  @Column(name = "ticket_id")
  private Long ticketId;

  @Column(name = "blocker_id")
  private Long blockerId;

  public TicketDependencyId() {}

  public TicketDependencyId(Long ticketId, Long blockerId) {
    this.ticketId = ticketId;
    this.blockerId = blockerId;
  }

  public Long getTicketId() {
    return ticketId;
  }

  public void setTicketId(Long ticketId) {
    this.ticketId = ticketId;
  }

  public Long getBlockerId() {
    return blockerId;
  }

  public void setBlockerId(Long blockerId) {
    this.blockerId = blockerId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TicketDependencyId other)) return false;
    return Objects.equals(ticketId, other.ticketId) && Objects.equals(blockerId, other.blockerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ticketId, blockerId);
  }
}
