package com.att.tdp.issueflow.project.repository;

import com.att.tdp.issueflow.project.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// Project persistence. findAllActive, findAllDeleted and findActiveById slice the table
// by soft delete state. The inherited findById still sees both kinds.
public interface ProjectRepository extends JpaRepository<Project, Long> {

  @Query("SELECT p FROM Project p WHERE p.deletedAt IS NULL")
  List<Project> findAllActive();

  @Query("SELECT p FROM Project p WHERE p.deletedAt IS NOT NULL")
  List<Project> findAllDeleted();

  @Query("SELECT p FROM Project p WHERE p.id = ?1 AND p.deletedAt IS NULL")
  Optional<Project> findActiveById(Long id);
}
