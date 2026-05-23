package com.att.tdp.issueflow.project.service;

import com.att.tdp.issueflow.project.dto.ProjectResponse;
import com.att.tdp.issueflow.project.entity.Project;

public final class ProjectMapper {

  private ProjectMapper() {}

  public static ProjectResponse toDto(Project p) {
    return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getOwnerId());
  }
}
