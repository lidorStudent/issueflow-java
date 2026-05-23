package com.att.tdp.issueflow.ticket.core.dto;

import com.att.tdp.issueflow.ticket.core.entity.TicketPriority;
import com.att.tdp.issueflow.ticket.core.entity.TicketStatus;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateTicketRequest(
    @Size(min = 1, max = 255) String title,
    @Size(max = 20_000) String description,
    TicketStatus status,
    TicketPriority priority,
    Long assigneeId,
    Instant dueDate) {}
