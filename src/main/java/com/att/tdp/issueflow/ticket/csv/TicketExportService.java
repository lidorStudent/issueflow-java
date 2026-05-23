package com.att.tdp.issueflow.ticket.csv;

import com.att.tdp.issueflow.ticket.core.entity.Ticket;
import com.att.tdp.issueflow.ticket.core.service.TicketService;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

// Streams the active tickets of a project as CSV. RFC 4180 handles commas and quotes
// inside fields automatically. Soft deleted tickets are skipped.
@Service
public class TicketExportService {

  private final TicketService tickets;

  public TicketExportService(TicketService tickets) {
    this.tickets = tickets;
  }

  public void writeCsv(long projectId, Writer writer) throws IOException {
    CSVFormat format =
        CSVFormat.RFC4180
            .builder()
            .setHeader("id", "title", "description", "status", "priority", "type", "assigneeId")
            .build();
    try (CSVPrinter printer = new CSVPrinter(writer, format)) {
      List<Ticket> rows = tickets.findActiveByProject(projectId);
      for (Ticket t : rows) {
        printer.printRecord(
            t.getId(),
            t.getTitle(),
            t.getDescription() == null ? "" : t.getDescription(),
            t.getStatus().name(),
            t.getPriority().name(),
            t.getType().name(),
            t.getAssigneeId() == null ? "" : t.getAssigneeId());
      }
    }
  }
}
