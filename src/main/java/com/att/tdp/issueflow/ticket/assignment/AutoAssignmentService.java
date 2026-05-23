package com.att.tdp.issueflow.ticket.assignment;

import com.att.tdp.issueflow.ticket.assignment.dto.WorkloadEntry;
import com.att.tdp.issueflow.user.entity.User;
import com.att.tdp.issueflow.user.entity.UserRole;
import com.att.tdp.issueflow.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Picks the least loaded developer for a new ticket and backs the /workload endpoint.
// Load means open tickets assigned to that user in this project (not DONE, not deleted).
// Ties break by registration order, so the older developer wins.
@Service
public class AutoAssignmentService {

  private final UserRepository userRepository;

  @PersistenceContext private EntityManager entityManager;

  public AutoAssignmentService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public Long pickLeastLoaded(Long projectId) {
    List<WorkloadEntry> ordered = workload(projectId);
    if (ordered.isEmpty()) {
      return null;
    }
    return ordered.get(0).userId();
  }

  // One entry per active developer, sorted by open ticket count ascending.
  // Ties are settled by registration order. The developer list is already sorted by
  // createdAt and List.sort is stable, so ties stay in that order.
  @Transactional(readOnly = true)
  public List<WorkloadEntry> workload(Long projectId) {
    List<User> developers = userRepository.findActiveByRoleOrderByCreatedAtAsc(UserRole.DEVELOPER);
    if (developers.isEmpty()) {
      return List.of();
    }
    // One aggregate query gives us the open ticket count per assignee for the project.
    @SuppressWarnings("unchecked")
    List<Object[]> rows =
        entityManager
            .createQuery(
                """
                        SELECT t.assigneeId, COUNT(t.id)
                        FROM Ticket t
                        WHERE t.projectId = :projectId
                          AND t.status <> com.att.tdp.issueflow.ticket.core.entity.TicketStatus.DONE
                          AND t.deletedAt IS NULL
                          AND t.assigneeId IS NOT NULL
                        GROUP BY t.assigneeId
                        """)
            .setParameter("projectId", projectId)
            .getResultList();
    Map<Long, Long> openTicketCountsByUserId = new HashMap<>();
    for (Object[] row : rows) {
      openTicketCountsByUserId.put((Long) row[0], (Long) row[1]);
    }
    List<WorkloadEntry> workload = new ArrayList<>(developers.size());
    for (User developer : developers) {
      workload.add(
          new WorkloadEntry(
              developer.getId(),
              developer.getUsername(),
              openTicketCountsByUserId.getOrDefault(developer.getId(), 0L)));
    }
    workload.sort(Comparator.comparingLong(WorkloadEntry::openTicketCount));
    return workload;
  }
}
