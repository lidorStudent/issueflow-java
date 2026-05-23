package com.att.tdp.issueflow.ticket.dependency.controller;

import com.att.tdp.issueflow.ticket.dependency.dto.AddDependencyRequest;
import com.att.tdp.issueflow.ticket.dependency.dto.DependencyResponse;
import com.att.tdp.issueflow.ticket.dependency.service.TicketDependencyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Endpoints for managing blocker edges on a ticket. Just add, list and remove.
// There is no PATCH because an edge has no fields other than its two endpoints.
@RestController
@RequestMapping("/tickets/{ticketId}/dependencies")
public class TicketDependencyController {

  private final TicketDependencyService service;

  public TicketDependencyController(TicketDependencyService service) {
    this.service = service;
  }

  @PostMapping
  public void add(@PathVariable long ticketId, @Valid @RequestBody AddDependencyRequest req) {
    service.add(ticketId, req.blockedBy());
  }

  @GetMapping
  public List<DependencyResponse> list(@PathVariable long ticketId) {
    return service.list(ticketId);
  }

  @DeleteMapping("/{blockerId}")
  public void remove(@PathVariable long ticketId, @PathVariable long blockerId) {
    service.remove(ticketId, blockerId);
  }
}
