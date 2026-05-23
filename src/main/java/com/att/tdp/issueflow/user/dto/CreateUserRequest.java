package com.att.tdp.issueflow.user.dto;

import com.att.tdp.issueflow.user.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Request body for POST /users.
// role is optional and defaults to DEVELOPER when absent. Matches the README contract.
// password is required by this implementation, see run.md for the rationale.
public record CreateUserRequest(
    @NotBlank
        @Size(min = 3, max = 64)
        @Pattern(
            regexp = "[A-Za-z0-9_.-]+",
            message = "username must contain only letters, digits, '_', '.', '-'")
        String username,
    @NotBlank @Email String email,
    @NotBlank @Size(max = 255) String fullName,
    UserRole role,
    @NotBlank @Size(min = 8, max = 128) String password) {}
