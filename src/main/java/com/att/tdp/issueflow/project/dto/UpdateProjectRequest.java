package com.att.tdp.issueflow.project.dto;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
    @Size(min = 1, max = 255) String name, @Size(max = 10_000) String description) {}
