package com.att.tdp.issueflow.ticket.core.service;

import com.att.tdp.issueflow.ticket.core.dto.TicketResponse;
import com.att.tdp.issueflow.ticket.core.entity.Ticket;

public final class TicketMapper {

  private TicketMapper() {}

  public static TicketResponse toDto(Ticket t) {
    return new TicketResponse(
        t.getId(),
        t.getTitle(),
        t.getDescription(),
        t.getStatus(),
        t.getPriority(),
        t.getType(),
        t.getProjectId(),
        t.getAssigneeId(),
        t.getDueDate(),
        t.isOverdue());
  }
}
