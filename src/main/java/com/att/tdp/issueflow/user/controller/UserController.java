package com.att.tdp.issueflow.user.controller;

import com.att.tdp.issueflow.user.dto.CreateUserRequest;
import com.att.tdp.issueflow.user.dto.UpdateUserRequest;
import com.att.tdp.issueflow.user.dto.UserResponse;
import com.att.tdp.issueflow.user.service.UserMapper;
import com.att.tdp.issueflow.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// User REST endpoints. Open shape matches the README contract: no role gating on registration
// or update. Authentication is still required for everything except POST /users (registration),
// which is wired as permitAll in SecurityConfig.
@RestController
@RequestMapping("/users")
public class UserController {

  private final UserService service;

  public UserController(UserService service) {
    this.service = service;
  }

  @GetMapping
  public List<UserResponse> list() {
    return service.findAll().stream().map(UserMapper::toDto).toList();
  }

  @GetMapping("/{userId}")
  public UserResponse get(@PathVariable long userId) {
    return UserMapper.toDto(service.findById(userId));
  }

  @PostMapping
  public UserResponse create(@Valid @RequestBody CreateUserRequest req) {
    return UserMapper.toDto(service.create(req));
  }

  // Legacy alias kept in the README contract. Same semantics as PATCH below.
  @PostMapping("/update/{userId}")
  public UserResponse updateLegacy(
      @PathVariable long userId, @Valid @RequestBody UpdateUserRequest req) {
    return UserMapper.toDto(service.update(userId, req));
  }

  @PatchMapping("/{userId}")
  public UserResponse update(@PathVariable long userId, @Valid @RequestBody UpdateUserRequest req) {
    return UserMapper.toDto(service.update(userId, req));
  }

  @DeleteMapping("/{userId}")
  public void delete(@PathVariable long userId) {
    service.delete(userId);
  }
}
