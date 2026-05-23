package com.att.tdp.issueflow.ticket.core.service;

import com.att.tdp.issueflow.audit.service.AuditService;
import com.att.tdp.issueflow.common.audit.AuditContext;
import com.att.tdp.issueflow.common.error.BusinessRuleException;
import com.att.tdp.issueflow.common.error.NotFoundException;
import com.att.tdp.issueflow.common.util.ETagSupport;
import com.att.tdp.issueflow.project.entity.Project;
import com.att.tdp.issueflow.project.service.ProjectService;
import com.att.tdp.issueflow.ticket.assignment.AutoAssignmentService;
import com.att.tdp.issueflow.ticket.core.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.core.dto.UpdateTicketRequest;
import com.att.tdp.issueflow.ticket.core.entity.Ticket;
import com.att.tdp.issueflow.ticket.core.entity.TicketPriority;
import com.att.tdp.issueflow.ticket.core.entity.TicketStatus;
import com.att.tdp.issueflow.ticket.core.repository.TicketRepository;
import com.att.tdp.issueflow.ticket.dependency.repository.TicketDependencyRepository;
import com.att.tdp.issueflow.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// All ticket business logic. Handles create, update, soft delete, restore, and the
// per-ticket escalation step the scheduler calls.
@Service
public class TicketService {

  private final TicketRepository ticketRepository;
  private final ProjectService projectService;
  private final UserRepository userRepository;
  private final AutoAssignmentService autoAssignmentService;
  private final TicketDependencyRepository ticketDependencyRepository;
  private final AuditService auditService;
  private final Clock clock;

  public TicketService(
      TicketRepository ticketRepository,
      ProjectService projectService,
      UserRepository userRepository,
      AutoAssignmentService autoAssignmentService,
      TicketDependencyRepository ticketDependencyRepository,
      AuditService auditService,
      Clock clock) {
    this.ticketRepository = ticketRepository;
    this.projectService = projectService;
    this.userRepository = userRepository;
    this.autoAssignmentService = autoAssignmentService;
    this.ticketDependencyRepository = ticketDependencyRepository;
    this.auditService = auditService;
    this.clock = clock;
  }

  // Create a ticket. If no assignee is given, pick the developer with the lightest load.
  // If one is given, just check the user is still active.
  @Transactional
  public Ticket create(CreateTicketRequest request) {
    Project project = projectService.findActive(request.projectId());
    // Explicit assignee gets validated, missing one triggers auto assign. Can stay null
    // if the project has no developers.
    Long assigneeId = request.assigneeId();
    if (assigneeId != null) {
      if (!userRepository.existsActiveById(assigneeId)) {
        throw NotFoundException.user(assigneeId);
      }
    } else {
      assigneeId = autoAssignmentService.pickLeastLoaded(project.getId());
    }
    Ticket ticket = new Ticket();
    ticket.setProjectId(project.getId());
    ticket.setTitle(request.title());
    ticket.setDescription(request.description());
    ticket.setStatus(request.status());
    ticket.setPriority(request.priority());
    ticket.setType(request.type());
    ticket.setAssigneeId(assigneeId);
    ticket.setDueDate(request.dueDate());
    ticket.setOverdue(false);
    Ticket saved = ticketRepository.save(ticket);
    auditService.recordCreate(
        "TICKET",
        saved.getId(),
        Map.of(
            "title", saved.getTitle(),
            "status", saved.getStatus().name(),
            "priority", saved.getPriority().name(),
            "type", saved.getType().name(),
            "projectId", saved.getProjectId(),
            "assigneeId",
                saved.getAssigneeId() == null ? Long.valueOf(-1L) : saved.getAssigneeId()));
    if (request.assigneeId() == null && assigneeId != null) {
      AuditContext.runAsSystem(
          () ->
              auditService.recordCustom(
                  "AUTO_ASSIGN",
                  "TICKET",
                  saved.getId(),
                  null,
                  Map.of("assigneeId", saved.getAssigneeId())));
    }
    return saved;
  }

  @Transactional(readOnly = true)
  public List<Ticket> findActiveByProject(long projectId) {
    projectService.findActive(projectId);
    return ticketRepository.findAllActiveByProject(projectId);
  }

  @Transactional(readOnly = true)
  public List<Ticket> findDeletedByProject(long projectId) {
    projectService.findAny(projectId);
    return ticketRepository.findAllDeletedByProject(projectId);
  }

  @Transactional(readOnly = true)
  public Ticket findActive(long id) {
    return ticketRepository.findActiveById(id).orElseThrow(() -> NotFoundException.ticket(id));
  }

  @Transactional(readOnly = true)
  public Ticket findAny(long id) {
    return ticketRepository.findById(id).orElseThrow(() -> NotFoundException.ticket(id));
  }

  // Patch a ticket. Three rules:
  //  1. If-Match version check, so two writers cannot stomp each other.
  //  2. DONE tickets are read only.
  //  3. Status only moves forward, and DONE needs all blockers resolved.
  // Changing priority by hand also resets the escalation state.
  @Transactional
  public Ticket update(long id, UpdateTicketRequest request, Long ifMatchVersion) {
    Ticket ticket = findActive(id);
    ETagSupport.verify(ifMatchVersion, ticket.getVersion());

    if (ticket.getStatus() == TicketStatus.DONE) {
      throw new BusinessRuleException(
          "TICKET_DONE_LOCKED", "Ticket is DONE and cannot be modified");
    }

    Map<String, Object> before = snapshot(ticket);

    if (request.title() != null) {
      ticket.setTitle(request.title());
    }
    if (request.description() != null) {
      ticket.setDescription(request.description());
    }
    if (request.dueDate() != null) {
      ticket.setDueDate(request.dueDate());
    }
    if (request.assigneeId() != null) {
      if (!userRepository.existsActiveById(request.assigneeId())) {
        throw NotFoundException.user(request.assigneeId());
      }
      ticket.setAssigneeId(request.assigneeId());
    }
    // Manual priority change resets escalation state. Next overdue sweep starts fresh.
    if (request.priority() != null && request.priority() != ticket.getPriority()) {
      ticket.setPriority(request.priority());
      ticket.setOverdue(false);
      ticket.setLastEscalatedAt(null);
    }
    // Status change. Validate the transition, and if moving to DONE, check no blockers are open.
    if (request.status() != null && request.status() != ticket.getStatus()) {
      StatusTransitionValidator.validate(ticket.getStatus(), request.status());
      if (request.status() == TicketStatus.DONE) {
        long unresolvedBlockerCount =
            ticketDependencyRepository.countUnresolvedBlockers(ticket.getId());
        if (unresolvedBlockerCount > 0) {
          throw new BusinessRuleException(
              "TICKET_BLOCKED",
              "Ticket cannot be moved to DONE: "
                  + unresolvedBlockerCount
                  + " unresolved blocker(s)");
        }
      }
      ticket.setStatus(request.status());
    }

    Map<String, Object> after = snapshot(ticket);
    auditService.recordUpdate("TICKET", ticket.getId(), before, after);
    return ticket;
  }

  @Transactional
  public void softDelete(long id) {
    Ticket ticket = findActive(id);
    ticket.setDeletedAt(Instant.now(clock));
    auditService.recordDelete("TICKET", id, Map.of("title", ticket.getTitle()));
  }

  // Restore a soft deleted ticket. We refuse if the parent project is also deleted,
  // otherwise the ticket would be visible while its project is not.
  @Transactional
  public void restore(long id) {
    Ticket ticket = findAny(id);
    if (!ticket.isDeleted()) {
      return;
    }
    Project project = projectService.findAny(ticket.getProjectId());
    if (project.isDeleted()) {
      throw new BusinessRuleException(
          "PROJECT_DELETED",
          "Cannot restore a ticket whose project is soft-deleted; restore the project first");
    }
    ticket.setDeletedAt(null);
    auditService.recordCustom(
        "RESTORE", "TICKET", id, null, Map.of("restoredAt", Instant.now(clock).toString()));
  }

  // One escalation step for one ticket. The scheduler calls this per overdue ticket.
  // Bumps priority up one level. Once at CRITICAL and still past due, flips isOverdue.
  // Safe to call again, CRITICAL never escalates further, DONE and no-due-date are skipped.
  @Transactional
  public Ticket applyEscalation(Long ticketId, Instant now) {
    Ticket ticket = findActive(ticketId);
    // Nothing to do if there is no due date, it is not past due yet, or the work is done.
    if (ticket.getDueDate() == null
        || !ticket.getDueDate().isBefore(now)
        || ticket.getStatus() == TicketStatus.DONE) {
      return ticket;
    }
    Map<String, Object> before =
        Map.of(
            "priority", ticket.getPriority().name(),
            "isOverdue", ticket.isOverdue());
    // Already CRITICAL. Do not promote further, just flip the overdue flag once.
    if (ticket.getPriority() == TicketPriority.CRITICAL) {
      if (!ticket.isOverdue()) {
        ticket.setOverdue(true);
        auditService.recordCustom(
            "AUTO_ESCALATE",
            "TICKET",
            ticket.getId(),
            before,
            Map.of("priority", ticket.getPriority().name(), "isOverdue", true));
      }
      return ticket;
    }
    TicketPriority nextPriority = ticket.getPriority().next();
    ticket.setPriority(nextPriority);
    ticket.setLastEscalatedAt(now);
    if (nextPriority == TicketPriority.CRITICAL) {
      ticket.setOverdue(true);
    }
    auditService.recordCustom(
        "AUTO_ESCALATE",
        "TICKET",
        ticket.getId(),
        before,
        Map.of("priority", ticket.getPriority().name(), "isOverdue", ticket.isOverdue()));
    return ticket;
  }

  // The shape we put in the audit log before and after maps. Just the user visible fields.
  private Map<String, Object> snapshot(Ticket ticket) {
    Map<String, Object> snapshot = new HashMap<>();
    snapshot.put("title", ticket.getTitle());
    snapshot.put("description", ticket.getDescription());
    snapshot.put("status", ticket.getStatus().name());
    snapshot.put("priority", ticket.getPriority().name());
    snapshot.put("assigneeId", ticket.getAssigneeId());
    snapshot.put("dueDate", ticket.getDueDate() == null ? null : ticket.getDueDate().toString());
    snapshot.put("isOverdue", ticket.isOverdue());
    return snapshot;
  }
}
