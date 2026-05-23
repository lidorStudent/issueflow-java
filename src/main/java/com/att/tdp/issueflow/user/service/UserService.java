package com.att.tdp.issueflow.user.service;

import com.att.tdp.issueflow.audit.service.AuditService;
import com.att.tdp.issueflow.common.error.ConflictException;
import com.att.tdp.issueflow.common.error.NotFoundException;
import com.att.tdp.issueflow.user.dto.CreateUserRequest;
import com.att.tdp.issueflow.user.dto.UpdateUserRequest;
import com.att.tdp.issueflow.user.entity.User;
import com.att.tdp.issueflow.user.entity.UserRole;
import com.att.tdp.issueflow.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// User CRUD with bcrypt password hashing and soft delete.
// Public POST /users always creates a DEVELOPER. Only admins change the role via PATCH.
// Delete is a soft delete so projects, tickets and comments that reference the user still work.
@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuditService auditService;
  private final Clock clock;

  public UserService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuditService auditService,
      Clock clock) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.auditService = auditService;
    this.clock = clock;
  }

  // Register a new user. Role is taken from the request when present, defaults to DEVELOPER.
  // Username and email checks are case insensitive and ignore soft deleted rows, so freed
  // names can be reused.
  @Transactional
  public User create(CreateUserRequest request) {
    if (userRepository.existsActiveByUsernameIgnoreCase(request.username())) {
      throw new ConflictException(
          "USERNAME_TAKEN", "Username '" + request.username() + "' is already taken");
    }
    if (userRepository.existsActiveByEmailIgnoreCase(request.email())) {
      throw new ConflictException(
          "EMAIL_TAKEN", "Email '" + request.email() + "' is already taken");
    }
    User user = new User();
    user.setUsername(request.username());
    user.setEmail(request.email());
    user.setFullName(request.fullName());
    user.setRole(request.role() == null ? UserRole.DEVELOPER : request.role());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    User saved = userRepository.save(user);
    auditService.recordCreate(
        "USER",
        saved.getId(),
        Map.of(
            "username", saved.getUsername(),
            "email", saved.getEmail(),
            "fullName", saved.getFullName(),
            "role", saved.getRole().name()));
    return saved;
  }

  @Transactional(readOnly = true)
  public List<User> findAll() {
    return userRepository.findAllActive();
  }

  @Transactional(readOnly = true)
  public User findById(long id) {
    return userRepository.findActiveById(id).orElseThrow(() -> NotFoundException.user(id));
  }

  @Transactional
  public User update(long id, UpdateUserRequest request) {
    User user = findById(id);
    Map<String, Object> before =
        Map.of("fullName", user.getFullName(), "role", user.getRole().name());
    if (request.fullName() != null) {
      user.setFullName(request.fullName());
    }
    if (request.role() != null) {
      user.setRole(request.role());
    }
    Map<String, Object> after =
        Map.of("fullName", user.getFullName(), "role", user.getRole().name());
    auditService.recordUpdate("USER", user.getId(), before, after);
    return user;
  }

  // Soft delete the user. A hard delete would break the FKs on projects.owner_id,
  // tickets.assignee_id and comments.author_id. This keeps history intact and hides the
  // user from normal lookups.
  @Transactional
  public void delete(long id) {
    User user = findById(id);
    user.setDeletedAt(Instant.now(clock));
    auditService.recordDelete("USER", id, Map.of("username", user.getUsername()));
  }
}
