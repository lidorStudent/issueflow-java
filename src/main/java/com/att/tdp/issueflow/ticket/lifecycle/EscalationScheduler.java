package com.att.tdp.issueflow.ticket.lifecycle;

import com.att.tdp.issueflow.common.audit.AuditContext;
import com.att.tdp.issueflow.config.EscalationProperties;
import com.att.tdp.issueflow.ticket.core.entity.Ticket;
import com.att.tdp.issueflow.ticket.core.repository.TicketRepository;
import com.att.tdp.issueflow.ticket.core.service.TicketService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Drives priority escalation for overdue tickets.
// Runs once at startup so a long downtime does not leave priorities stale, then on the
// configured cron. Each ticket is escalated in its own transaction inside TicketService,
// so one bad row cannot take down the whole batch.
@Component
public class EscalationScheduler {

  private static final Logger log = LoggerFactory.getLogger(EscalationScheduler.class);

  private final TicketRepository ticketRepository;
  private final TicketService ticketService;
  private final EscalationProperties properties;
  private final Clock clock;

  public EscalationScheduler(
      TicketRepository ticketRepository,
      TicketService ticketService,
      EscalationProperties properties,
      Clock clock) {
    this.ticketRepository = ticketRepository;
    this.ticketService = ticketService;
    this.properties = properties;
    this.clock = clock;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onStartup() {
    if (properties.runOnStartup()) {
      runOnce();
    }
  }

  @Scheduled(cron = "${issueflow.escalation.cron}")
  public void runScheduled() {
    runOnce();
  }

  // Find overdue tickets and try to escalate each one.
  // Wrapped in AuditContext so audit rows here are tagged as SYSTEM.
  // Per ticket errors are logged and swallowed, one bad ticket should not stop the rest.
  public void runOnce() {
    Instant now = Instant.now(clock);
    List<Ticket> overdueCandidates = ticketRepository.findOverdueCandidates(now);
    if (overdueCandidates.isEmpty()) {
      return;
    }
    log.info("Escalation: processing {} overdue tickets", overdueCandidates.size());
    AuditContext.runAsSystem(
        () -> {
          for (Ticket ticket : overdueCandidates) {
            try {
              ticketService.applyEscalation(ticket.getId(), now);
            } catch (RuntimeException ex) {
              // Do not abort the batch. Log and move on.
              log.warn("Escalation failed for ticket {}: {}", ticket.getId(), ex.getMessage());
            }
          }
        });
  }
}
