package com.att.tdp.issueflow.project.service;

import com.att.tdp.issueflow.audit.service.AuditService;
import com.att.tdp.issueflow.common.error.NotFoundException;
import com.att.tdp.issueflow.common.util.ETagSupport;
import com.att.tdp.issueflow.project.dto.CreateProjectRequest;
import com.att.tdp.issueflow.project.dto.UpdateProjectRequest;
import com.att.tdp.issueflow.project.entity.Project;
import com.att.tdp.issueflow.project.repository.ProjectRepository;
import com.att.tdp.issueflow.ticket.core.repository.TicketRepository;
import com.att.tdp.issueflow.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// CRUD and soft delete for projects. When we soft delete a project we stamp the same
// deleted_at on its active tickets, so restoring the project brings exactly those tickets
// back. Tickets deleted on their own keep their own timestamp and stay deleted.
@Service
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final TicketRepository ticketRepository;
  private final AuditService auditService;
  private final Clock clock;

  public ProjectService(
      ProjectRepository projectRepository,
      UserRepository userRepository,
      TicketRepository ticketRepository,
      AuditService auditService,
      Clock clock) {
    this.projectRepository = projectRepository;
    this.userRepository = userRepository;
    this.ticketRepository = ticketRepository;
    this.auditService = auditService;
    this.clock = clock;
  }

  @Transactional
  public Project create(CreateProjectRequest request) {
    if (!userRepository.existsActiveById(request.ownerId())) {
      throw NotFoundException.user(request.ownerId());
    }
    Project project = new Project();
    project.setName(request.name());
    project.setDescription(request.description());
    project.setOwnerId(request.ownerId());
    Project saved = projectRepository.save(project);
    auditService.recordCreate(
        "PROJECT",
        saved.getId(),
        Map.of(
            "name", saved.getName(),
            "description", saved.getDescription() == null ? "" : saved.getDescription(),
            "ownerId", saved.getOwnerId()));
    return saved;
  }

  @Transactional(readOnly = true)
  public List<Project> findAllActive() {
    return projectRepository.findAllActive();
  }

  @Transactional(readOnly = true)
  public List<Project> findAllDeleted() {
    return projectRepository.findAllDeleted();
  }

  @Transactional(readOnly = true)
  public Project findActive(long id) {
    return projectRepository.findActiveById(id).orElseThrow(() -> NotFoundException.project(id));
  }

  @Transactional(readOnly = true)
  public Project findAny(long id) {
    return projectRepository.findById(id).orElseThrow(() -> NotFoundException.project(id));
  }

  @Transactional
  public Project update(long id, UpdateProjectRequest request, Long ifMatchVersion) {
    Project project = findActive(id);
    ETagSupport.verify(ifMatchVersion, project.getVersion());
    Map<String, Object> before = new HashMap<>();
    before.put("name", project.getName());
    before.put("description", project.getDescription());
    if (request.name() != null) {
      project.setName(request.name());
    }
    if (request.description() != null) {
      project.setDescription(request.description());
    }
    Map<String, Object> after = new HashMap<>();
    after.put("name", project.getName());
    after.put("description", project.getDescription());
    auditService.recordUpdate("PROJECT", project.getId(), before, after);
    return project;
  }

  // Soft delete a project. Stamp the same deleted_at on its active tickets. The shared
  // timestamp is how restore knows which tickets to bring back later.
  @Transactional
  public void softDelete(long id) {
    Project project = findActive(id);
    Instant now = Instant.now(clock);
    project.setDeletedAt(now);
    ticketRepository.cascadeSoftDelete(id, now);
    auditService.recordDelete("PROJECT", id, Map.of("name", project.getName()));
  }

  // Restore a soft deleted project. Bring back only the tickets that were deleted with it
  // (matched by the shared timestamp). Tickets deleted on their own stay deleted.
  @Transactional
  public void restore(long id) {
    Project project = findAny(id);
    if (!project.isDeleted()) {
      return;
    }
    Instant deletedAt = project.getDeletedAt();
    project.setDeletedAt(null);
    ticketRepository.cascadeRestore(id, deletedAt);
    auditService.recordCustom(
        "RESTORE", "PROJECT", id, null, Map.of("restoredAt", Instant.now(clock).toString()));
  }
}
