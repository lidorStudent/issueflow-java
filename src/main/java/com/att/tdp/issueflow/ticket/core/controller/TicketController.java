package com.att.tdp.issueflow.ticket.core.controller;

import com.att.tdp.issueflow.common.util.ETagSupport;
import com.att.tdp.issueflow.ticket.core.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.core.dto.TicketResponse;
import com.att.tdp.issueflow.ticket.core.dto.UpdateTicketRequest;
import com.att.tdp.issueflow.ticket.core.entity.Ticket;
import com.att.tdp.issueflow.ticket.core.service.TicketMapper;
import com.att.tdp.issueflow.ticket.core.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Ticket REST endpoints. GET returns an ETag header, PATCH expects the same value back as
// If-Match for concurrency control. /deleted and /restore are admin only.
@RestController
@RequestMapping("/tickets")
public class TicketController {

  private final TicketService service;

  public TicketController(TicketService service) {
    this.service = service;
  }

  @GetMapping
  public List<TicketResponse> listByProject(@RequestParam("projectId") long projectId) {
    return service.findActiveByProject(projectId).stream().map(TicketMapper::toDto).toList();
  }

  @GetMapping("/deleted")
  @PreAuthorize("hasRole('ADMIN')")
  public List<TicketResponse> listDeleted(@RequestParam("projectId") long projectId) {
    return service.findDeletedByProject(projectId).stream().map(TicketMapper::toDto).toList();
  }

  @GetMapping("/{ticketId}")
  public ResponseEntity<TicketResponse> get(@PathVariable long ticketId) {
    Ticket t = service.findActive(ticketId);
    return ResponseEntity.ok()
        .headers(ETagSupport.headers(t.getVersion()))
        .body(TicketMapper.toDto(t));
  }

  @PostMapping
  public TicketResponse create(@Valid @RequestBody CreateTicketRequest req) {
    return TicketMapper.toDto(service.create(req));
  }

  @PatchMapping("/{ticketId}")
  public ResponseEntity<TicketResponse> update(
      @PathVariable long ticketId,
      @Valid @RequestBody UpdateTicketRequest req,
      @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
    Long version = ETagSupport.parseIfMatch(ifMatch);
    Ticket t = service.update(ticketId, req, version);
    return ResponseEntity.ok()
        .headers(ETagSupport.headers(t.getVersion()))
        .body(TicketMapper.toDto(t));
  }

  @DeleteMapping("/{ticketId}")
  public void delete(@PathVariable long ticketId) {
    service.softDelete(ticketId);
  }

  @PostMapping("/{ticketId}/restore")
  @PreAuthorize("hasRole('ADMIN')")
  public void restore(@PathVariable long ticketId) {
    service.restore(ticketId);
  }
}
