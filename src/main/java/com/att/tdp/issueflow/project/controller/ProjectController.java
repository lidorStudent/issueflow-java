package com.att.tdp.issueflow.project.controller;

import com.att.tdp.issueflow.common.util.ETagSupport;
import com.att.tdp.issueflow.project.dto.CreateProjectRequest;
import com.att.tdp.issueflow.project.dto.ProjectResponse;
import com.att.tdp.issueflow.project.dto.UpdateProjectRequest;
import com.att.tdp.issueflow.project.entity.Project;
import com.att.tdp.issueflow.project.service.ProjectMapper;
import com.att.tdp.issueflow.project.service.ProjectService;
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
import org.springframework.web.bind.annotation.RestController;

// Project REST endpoints. Same shape as the ticket controller: ETag and If-Match on PATCH,
// admin only access for /deleted and /restore.
@RestController
@RequestMapping("/projects")
public class ProjectController {

  private final ProjectService service;

  public ProjectController(ProjectService service) {
    this.service = service;
  }

  @GetMapping
  public List<ProjectResponse> list() {
    return service.findAllActive().stream().map(ProjectMapper::toDto).toList();
  }

  @GetMapping("/deleted")
  @PreAuthorize("hasRole('ADMIN')")
  public List<ProjectResponse> listDeleted() {
    return service.findAllDeleted().stream().map(ProjectMapper::toDto).toList();
  }

  @GetMapping("/{projectId}")
  public ResponseEntity<ProjectResponse> get(@PathVariable long projectId) {
    Project p = service.findActive(projectId);
    return ResponseEntity.ok()
        .headers(ETagSupport.headers(p.getVersion()))
        .body(ProjectMapper.toDto(p));
  }

  @PostMapping
  public ProjectResponse create(@Valid @RequestBody CreateProjectRequest req) {
    return ProjectMapper.toDto(service.create(req));
  }

  @PatchMapping("/{projectId}")
  public ResponseEntity<ProjectResponse> update(
      @PathVariable long projectId,
      @Valid @RequestBody UpdateProjectRequest req,
      @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
    Long version = ETagSupport.parseIfMatch(ifMatch);
    Project p = service.update(projectId, req, version);
    return ResponseEntity.ok()
        .headers(ETagSupport.headers(p.getVersion()))
        .body(ProjectMapper.toDto(p));
  }

  @DeleteMapping("/{projectId}")
  public void delete(@PathVariable long projectId) {
    service.softDelete(projectId);
  }

  @PostMapping("/{projectId}/restore")
  @PreAuthorize("hasRole('ADMIN')")
  public void restore(@PathVariable long projectId) {
    service.restore(projectId);
  }
}
