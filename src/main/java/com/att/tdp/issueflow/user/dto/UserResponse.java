package com.att.tdp.issueflow.user.dto;

import com.att.tdp.issueflow.user.entity.UserRole;

public record UserResponse(
    Long id, String username, String email, String fullName, UserRole role) {}
