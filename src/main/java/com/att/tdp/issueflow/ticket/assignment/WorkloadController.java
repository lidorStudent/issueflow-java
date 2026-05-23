package com.att.tdp.issueflow.ticket.assignment;

import com.att.tdp.issueflow.project.service.ProjectService;
import com.att.tdp.issueflow.ticket.assignment.dto.WorkloadEntry;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Reports the per developer open ticket load for a project.
// Checks the project exists, then delegates to AutoAssignmentService.
@RestController
@RequestMapping("/projects/{projectId}")
public class WorkloadController {

  private final AutoAssignmentService autoAssign;
  private final ProjectService projects;

  public WorkloadController(AutoAssignmentService autoAssign, ProjectService projects) {
    this.autoAssign = autoAssign;
    this.projects = projects;
  }

  @GetMapping("/workload")
  public List<WorkloadEntry> workload(@PathVariable long projectId) {
    projects.findActive(projectId);
    return autoAssign.workload(projectId);
  }
}
