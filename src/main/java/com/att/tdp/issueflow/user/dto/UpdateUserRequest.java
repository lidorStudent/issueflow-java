package com.att.tdp.issueflow.user.dto;

import com.att.tdp.issueflow.user.entity.UserRole;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(@Size(min = 1, max = 255) String fullName, UserRole role) {}
