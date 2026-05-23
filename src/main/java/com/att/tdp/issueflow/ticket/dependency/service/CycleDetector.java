package com.att.tdp.issueflow.ticket.dependency.service;

import com.att.tdp.issueflow.ticket.dependency.repository.TicketDependencyRepository;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

// Cycle check for the dependency graph. Loads the project adjacency in one query, then
// walks it in memory. Avoids hitting the DB once per node.
@Component
public class CycleDetector {

  private final TicketDependencyRepository deps;

  public CycleDetector(TicketDependencyRepository deps) {
    this.deps = deps;
  }

  // Returns true if adding the edge ticketId -> newBlockerId would close a cycle.
  // We start DFS at the new blocker and check if we can reach ticketId from there.
  // The visited set is also a safety net against any pre-existing cycle in the graph.
  public boolean wouldCreateCycle(long projectId, long ticketId, long newBlockerId) {
    if (ticketId == newBlockerId) {
      return true;
    }
    Map<Long, List<Long>> adjacency = loadAdjacency(projectId);
    Deque<Long> stack = new ArrayDeque<>();
    Set<Long> visited = new HashSet<>();
    stack.push(newBlockerId);
    while (!stack.isEmpty()) {
      long current = stack.pop();
      if (!visited.add(current)) {
        continue;
      }
      if (current == ticketId) {
        return true;
      }
      for (Long blocker : adjacency.getOrDefault(current, List.of())) {
        stack.push(blocker);
      }
    }
    return false;
  }

  // Load every ticketId, blockerId pair in the project as an in memory adjacency map.
  private Map<Long, List<Long>> loadAdjacency(long projectId) {
    List<Object[]> rows = deps.findAdjacencyByProject(projectId);
    Map<Long, List<Long>> adjacency = new HashMap<>();
    for (Object[] row : rows) {
      Long ticketId = (Long) row[0];
      Long blockerId = (Long) row[1];
      adjacency.computeIfAbsent(ticketId, k -> new java.util.ArrayList<>()).add(blockerId);
    }
    return adjacency;
  }
}
