package com.att.tdp.issueflow.ticket.core.dto;

import com.att.tdp.issueflow.ticket.core.entity.TicketPriority;
import com.att.tdp.issueflow.ticket.core.entity.TicketStatus;
import com.att.tdp.issueflow.ticket.core.entity.TicketType;
import java.time.Instant;

public record TicketResponse(
    Long id,
    String title,
    String description,
    TicketStatus status,
    TicketPriority priority,
    TicketType type,
    Long projectId,
    Long assigneeId,
    Instant dueDate,
    boolean isOverdue) {}
