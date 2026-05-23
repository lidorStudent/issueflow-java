package com.att.tdp.issueflow.user.repository;

import com.att.tdp.issueflow.user.entity.User;
import com.att.tdp.issueflow.user.entity.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// User persistence. All reads here filter out soft deleted rows by default.
// If a caller actually needs the raw row (for example to show the author of an old comment),
// it can still use the inherited findById from JpaRepository.
// Username and email checks are case insensitive so the same name in different case cannot
// be registered twice.
public interface UserRepository extends JpaRepository<User, Long> {

  @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
  List<User> findAllActive();

  @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
  Optional<User> findActiveById(@Param("id") Long id);

  @Query("SELECT (COUNT(u) > 0) FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
  boolean existsActiveById(@Param("id") Long id);

  @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:username) AND u.deletedAt IS NULL")
  Optional<User> findActiveByUsernameIgnoreCase(@Param("username") String username);

  @Query("SELECT u FROM User u WHERE LOWER(u.username) IN :usernames AND u.deletedAt IS NULL")
  List<User> findActiveByUsernameInIgnoreCase(@Param("usernames") Collection<String> usernames);

  @Query(
      "SELECT (COUNT(u) > 0) FROM User u WHERE LOWER(u.username) = LOWER(:username) AND u.deletedAt IS NULL")
  boolean existsActiveByUsernameIgnoreCase(@Param("username") String username);

  @Query(
      "SELECT (COUNT(u) > 0) FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.deletedAt IS NULL")
  boolean existsActiveByEmailIgnoreCase(@Param("email") String email);

  @Query(
      "SELECT u FROM User u WHERE u.role = :role AND u.deletedAt IS NULL ORDER BY u.createdAt ASC")
  List<User> findActiveByRoleOrderByCreatedAtAsc(@Param("role") UserRole role);
}
