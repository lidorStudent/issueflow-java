package com.att.tdp.issueflow.ticket.core.repository;

import com.att.tdp.issueflow.ticket.core.entity.Ticket;
import com.att.tdp.issueflow.ticket.core.entity.TicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Ticket persistence. All the public queries filter out soft deleted rows.
// The cascade queries are the exception, they target deleted or active rows on purpose
// and are only called from ProjectService when a project is deleted or restored.
public interface TicketRepository extends JpaRepository<Ticket, Long> {

  @Query("SELECT t FROM Ticket t WHERE t.projectId = :projectId AND t.deletedAt IS NULL")
  List<Ticket> findAllActiveByProject(@Param("projectId") Long projectId);

  @Query("SELECT t FROM Ticket t WHERE t.projectId = :projectId AND t.deletedAt IS NOT NULL")
  List<Ticket> findAllDeletedByProject(@Param("projectId") Long projectId);

  @Query("SELECT t FROM Ticket t WHERE t.id = :id AND t.deletedAt IS NULL")
  Optional<Ticket> findActiveById(@Param("id") Long id);

  @Query(
      "SELECT t FROM Ticket t WHERE t.dueDate IS NOT NULL AND t.dueDate < :now AND t.status <> 'DONE' AND t.deletedAt IS NULL")
  List<Ticket> findOverdueCandidates(@Param("now") Instant now);

  @Query(
      "SELECT t FROM Ticket t WHERE t.projectId = :projectId AND t.status <> :doneStatus AND t.deletedAt IS NULL AND t.assigneeId IS NOT NULL")
  List<Ticket> findOpenByProject(
      @Param("projectId") Long projectId, @Param("doneStatus") TicketStatus doneStatus);

  // Stamp the project's deleted_at onto its currently active tickets.
  @Modifying
  @Query(
      "UPDATE Ticket t SET t.deletedAt = :when WHERE t.projectId = :projectId AND t.deletedAt IS NULL")
  int cascadeSoftDelete(@Param("projectId") Long projectId, @Param("when") Instant when);

  // The inverse. Only restore tickets that were deleted together with the project
  // (matched by the same deleted_at timestamp).
  @Modifying
  @Query(
      "UPDATE Ticket t SET t.deletedAt = NULL WHERE t.projectId = :projectId AND t.deletedAt = :deletedAt")
  int cascadeRestore(@Param("projectId") Long projectId, @Param("deletedAt") Instant deletedAt);
}
