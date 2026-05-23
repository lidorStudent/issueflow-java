package com.att.tdp.issueflow.ticket.core.service;

import com.att.tdp.issueflow.common.error.BusinessRuleException;
import com.att.tdp.issueflow.ticket.core.entity.TicketStatus;

// Enforces the ticket status FSM. Forward only, and DONE is terminal.
// Skipping a level (for example straight to DONE from the initial state) is allowed.
// The spec only forbids going backwards.
public final class StatusTransitionValidator {

  private StatusTransitionValidator() {}

  // Same state is a no-op. Throws BusinessRuleException on backward moves or any change
  // out of DONE.
  public static void validate(TicketStatus from, TicketStatus to) {
    if (from == to) {
      return;
    }
    if (from == TicketStatus.DONE) {
      throw new BusinessRuleException(
          "TICKET_DONE_LOCKED", "Ticket is DONE and cannot be modified");
    }
    if (!to.isAfter(from)) {
      throw new BusinessRuleException(
          "INVALID_STATUS_TRANSITION",
          "Cannot transition from "
              + from
              + " to "
              + to
              + " (backward transitions are not allowed)");
    }
  }
}
