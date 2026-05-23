package com.att.tdp.issueflow.ticket.dependency.service;

import com.att.tdp.issueflow.audit.service.AuditService;
import com.att.tdp.issueflow.common.error.BusinessRuleException;
import com.att.tdp.issueflow.common.error.NotFoundException;
import com.att.tdp.issueflow.ticket.core.entity.Ticket;
import com.att.tdp.issueflow.ticket.core.repository.TicketRepository;
import com.att.tdp.issueflow.ticket.core.service.TicketService;
import com.att.tdp.issueflow.ticket.dependency.dto.DependencyResponse;
import com.att.tdp.issueflow.ticket.dependency.entity.TicketDependency;
import com.att.tdp.issueflow.ticket.dependency.entity.TicketDependencyId;
import com.att.tdp.issueflow.ticket.dependency.repository.TicketDependencyRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Adds, removes and lists blocker edges between tickets. Refuses anything that would
// create a cycle or cross project boundaries.
@Service
public class TicketDependencyService {

  private final TicketDependencyRepository ticketDependencyRepository;
  private final TicketService ticketService;
  private final TicketRepository ticketRepository;
  private final CycleDetector cycleDetector;
  private final AuditService auditService;
  private final Clock clock;

  public TicketDependencyService(
      TicketDependencyRepository ticketDependencyRepository,
      TicketService ticketService,
      TicketRepository ticketRepository,
      CycleDetector cycleDetector,
      AuditService auditService,
      Clock clock) {
    this.ticketDependencyRepository = ticketDependencyRepository;
    this.ticketService = ticketService;
    this.ticketRepository = ticketRepository;
    this.cycleDetector = cycleDetector;
    this.auditService = auditService;
    this.clock = clock;
  }

  // Add a "ticket is blocked by blocker" edge. Checks no self loop, both tickets active
  // and in the same project, and that the new edge does not close a cycle.
  // Adding the same edge twice is a no-op so retries are safe.
  @Transactional
  public void add(long ticketId, long blockerId) {
    if (ticketId == blockerId) {
      throw new BusinessRuleException("DEPENDENCY_SELF", "A ticket cannot depend on itself");
    }
    Ticket ticket = ticketService.findActive(ticketId);
    Ticket blocker = ticketService.findActive(blockerId);
    if (!ticket.getProjectId().equals(blocker.getProjectId())) {
      throw new BusinessRuleException(
          "DEPENDENCY_CROSS_PROJECT", "Both tickets must belong to the same project");
    }
    if (cycleDetector.wouldCreateCycle(ticket.getProjectId(), ticketId, blockerId)) {
      throw new BusinessRuleException(
          "DEPENDENCY_CYCLE", "Adding this dependency would create a cycle");
    }
    TicketDependency entity = new TicketDependency(ticketId, blockerId, Instant.now(clock));
    // Edge already exists, nothing to add and no extra audit entry.
    if (ticketDependencyRepository.existsById(entity.getId())) {
      return;
    }
    ticketDependencyRepository.save(entity);
    auditService.recordCustom(
        "ADD_DEPENDENCY", "TICKET", ticketId, null, Map.of("blockedBy", blockerId));
  }

  // Return the blockers as id, title, status. Uses one batched findAllById to avoid
  // an N+1 lookup per blocker.
  @Transactional(readOnly = true)
  public List<DependencyResponse> list(long ticketId) {
    ticketService.findActive(ticketId);
    List<Long> blockerIds = ticketDependencyRepository.findBlockerIds(ticketId);
    if (blockerIds.isEmpty()) {
      return List.of();
    }
    // Load all blockers in one query, then keep the original order by iterating the id list.
    Map<Long, Ticket> blockersById =
        ticketRepository.findAllById(blockerIds).stream()
            .collect(Collectors.toMap(Ticket::getId, ticket -> ticket));
    return blockerIds.stream()
        .map(blockersById::get)
        .filter(Objects::nonNull)
        .map(
            blocker ->
                new DependencyResponse(blocker.getId(), blocker.getTitle(), blocker.getStatus()))
        .toList();
  }

  @Transactional
  public void remove(long ticketId, long blockerId) {
    TicketDependencyId id = new TicketDependencyId(ticketId, blockerId);
    if (!ticketDependencyRepository.existsById(id)) {
      throw new NotFoundException(
          "DEPENDENCY_NOT_FOUND",
          "Dependency from ticket " + ticketId + " to blocker " + blockerId + " not found");
    }
    ticketDependencyRepository.deleteById(id);
    auditService.recordCustom(
        "REMOVE_DEPENDENCY", "TICKET", ticketId, Map.of("blockedBy", blockerId), null);
  }
}
