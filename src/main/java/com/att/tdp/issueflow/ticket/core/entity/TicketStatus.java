package com.att.tdp.issueflow.ticket.core.entity;

// Ticket lifecycle states. The numeric order is what the FSM validator uses to check
// whether a given transition is moving forward.
public enum TicketStatus {
  TODO(0),
  IN_PROGRESS(1),
  IN_REVIEW(2),
  DONE(3);

  private final int order;

  TicketStatus(int order) {
    this.order = order;
  }

  public int order() {
    return order;
  }

  public boolean isAfter(TicketStatus other) {
    return this.order > other.order;
  }
}
