package com.att.tdp.issueflow.ticket.dependency.repository;

import com.att.tdp.issueflow.ticket.dependency.entity.TicketDependency;
import com.att.tdp.issueflow.ticket.dependency.entity.TicketDependencyId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Persistence for the (ticket, blocker) edge table.
// Has forward and reverse adjacency lookups plus the project scoped graph load used by
// CycleDetector.
public interface TicketDependencyRepository
    extends JpaRepository<TicketDependency, TicketDependencyId> {

  @Query("SELECT d.id.blockerId FROM TicketDependency d WHERE d.id.ticketId = :ticketId")
  List<Long> findBlockerIds(@Param("ticketId") Long ticketId);

  @Query("SELECT d.id.ticketId FROM TicketDependency d WHERE d.id.blockerId = :blockerId")
  List<Long> findDependentIds(@Param("blockerId") Long blockerId);

  @Query(
      """
            SELECT d.id.ticketId, d.id.blockerId
            FROM TicketDependency d
            WHERE d.id.ticketId IN (SELECT t.id FROM Ticket t WHERE t.projectId = :projectId)
            """)
  List<Object[]> findAdjacencyByProject(@Param("projectId") Long projectId);

  // Count blockers for ticketId that are not DONE and not soft deleted. These are the
  // ones that would stop the ticket from moving to DONE. A soft deleted blocker does not
  // block anymore.
  @Query(
      """
            SELECT COUNT(t)
            FROM Ticket t
            WHERE t.id IN (SELECT d.id.blockerId FROM TicketDependency d WHERE d.id.ticketId = :ticketId)
              AND t.status <> com.att.tdp.issueflow.ticket.core.entity.TicketStatus.DONE
              AND t.deletedAt IS NULL
            """)
  long countUnresolvedBlockers(@Param("ticketId") Long ticketId);
}
