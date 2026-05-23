package com.att.tdp.issueflow.user.service;

import com.att.tdp.issueflow.user.dto.UserResponse;
import com.att.tdp.issueflow.user.entity.User;

public final class UserMapper {

  private UserMapper() {}

  public static UserResponse toDto(User u) {
    return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.getRole());
  }
}
