package com.att.tdp.issueflow.ticket.dependency.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.att.tdp.issueflow.ticket.dependency.repository.TicketDependencyRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CycleDetectorTest {

  private static final long PROJECT_ID = 100L;

  @Test
  void selfDependencyIsCycle() {
    TicketDependencyRepository repo = mock(TicketDependencyRepository.class);
    CycleDetector detector = new CycleDetector(repo);
    assertThat(detector.wouldCreateCycle(PROJECT_ID, 1L, 1L)).isTrue();
  }

  @Test
  void directBackEdgeIsCycle() {
    TicketDependencyRepository repo = mock(TicketDependencyRepository.class);
    when(repo.findAdjacencyByProject(PROJECT_ID)).thenReturn(adjacency(edge(2L, 1L)));
    CycleDetector detector = new CycleDetector(repo);
    assertThat(detector.wouldCreateCycle(PROJECT_ID, 1L, 2L)).isTrue();
  }

  @Test
  void transitiveBackEdgeIsCycle() {
    TicketDependencyRepository repo = mock(TicketDependencyRepository.class);
    when(repo.findAdjacencyByProject(PROJECT_ID)).thenReturn(adjacency(edge(3L, 2L), edge(2L, 1L)));
    CycleDetector detector = new CycleDetector(repo);
    assertThat(detector.wouldCreateCycle(PROJECT_ID, 1L, 3L)).isTrue();
  }

  @Test
  void unrelatedChainIsNotCycle() {
    TicketDependencyRepository repo = mock(TicketDependencyRepository.class);
    when(repo.findAdjacencyByProject(PROJECT_ID)).thenReturn(adjacency(edge(2L, 3L)));
    CycleDetector detector = new CycleDetector(repo);
    assertThat(detector.wouldCreateCycle(PROJECT_ID, 1L, 2L)).isFalse();
  }

  @Test
  void handlesPreexistingCycleInGraphWithoutInfiniteLoop() {
    TicketDependencyRepository repo = mock(TicketDependencyRepository.class);
    when(repo.findAdjacencyByProject(PROJECT_ID)).thenReturn(adjacency(edge(2L, 3L), edge(3L, 2L)));
    CycleDetector detector = new CycleDetector(repo);
    assertThat(detector.wouldCreateCycle(PROJECT_ID, 1L, 2L)).isFalse();
  }

  private static Object[] edge(long ticketId, long blockerId) {
    return new Object[] {ticketId, blockerId};
  }

  private static List<Object[]> adjacency(Object[]... edges) {
    List<Object[]> rows = new ArrayList<>();
    rows.addAll(Arrays.asList(edges));
    return rows;
  }
}
