package com.att.tdp.issueflow.ticket.csv;

import com.att.tdp.issueflow.ticket.core.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.core.service.TicketService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Per row create wrapper for CSV import. Has to be a separate bean so REQUIRES_NEW actually
// opens a new transaction. Calling a @Transactional method on the same bean would bypass
// the proxy and join the outer tx. Each row succeeds or fails on its own.
@Component
public class CsvRowImporter {

  private final TicketService tickets;

  public CsvRowImporter(TicketService tickets) {
    this.tickets = tickets;
  }

  // REQUIRES_NEW means one bad row rolls back only itself, not the whole import.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createOne(CreateTicketRequest req) {
    tickets.create(req);
  }
}
