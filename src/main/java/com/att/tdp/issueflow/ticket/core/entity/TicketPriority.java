package com.att.tdp.issueflow.ticket.core.entity;

// Ticket priority levels. next() drives the auto escalation ladder.
// CRITICAL is the top, so calling next() on it stays at CRITICAL.
public enum TicketPriority {
  LOW,
  MEDIUM,
  HIGH,
  CRITICAL;

  public TicketPriority next() {
    return switch (this) {
      case LOW -> MEDIUM;
      case MEDIUM -> HIGH;
      case HIGH, CRITICAL -> CRITICAL;
    };
  }
}
