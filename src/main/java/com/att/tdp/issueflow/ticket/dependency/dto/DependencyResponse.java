package com.att.tdp.issueflow.ticket.dependency.dto;

import com.att.tdp.issueflow.ticket.core.entity.TicketStatus;

public record DependencyResponse(Long id, String title, TicketStatus status) {}
