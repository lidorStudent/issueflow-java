package com.att.tdp.issueflow.ticket.csv;

import com.att.tdp.issueflow.ticket.csv.dto.ImportSummary;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

// CSV import and export endpoints.
// Export uses StreamingResponseBody so we do not materialise the whole file in memory
// before sending. Import returns a summary so the client can show per row outcomes.
@RestController
@RequestMapping("/tickets")
public class TicketCsvController {

  private final TicketExportService exportService;
  private final TicketImportService importService;

  public TicketCsvController(TicketExportService exportService, TicketImportService importService) {
    this.exportService = exportService;
    this.importService = importService;
  }

  @GetMapping(value = "/export", produces = "text/csv")
  public ResponseEntity<StreamingResponseBody> export(
      @RequestParam("projectId") long projectId, HttpServletResponse response) {
    response.setHeader(
        "Content-Disposition", "attachment; filename=tickets-project-" + projectId + ".csv");
    StreamingResponseBody body =
        out -> {
          try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            exportService.writeCsv(projectId, w);
          }
        };
    return ResponseEntity.ok().body(body);
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ImportSummary importCsv(
      @RequestParam("file") MultipartFile file, @RequestParam("projectId") long projectId)
      throws IOException {
    return importService.importCsv(projectId, file);
  }
}
