package com.att.tdp.issueflow.ticket.csv;

import com.att.tdp.issueflow.audit.service.AuditService;
import com.att.tdp.issueflow.project.service.ProjectService;
import com.att.tdp.issueflow.ticket.core.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.core.entity.TicketPriority;
import com.att.tdp.issueflow.ticket.core.entity.TicketStatus;
import com.att.tdp.issueflow.ticket.core.entity.TicketType;
import com.att.tdp.issueflow.ticket.csv.dto.ImportError;
import com.att.tdp.issueflow.ticket.csv.dto.ImportSummary;
import com.att.tdp.issueflow.user.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// Bulk imports tickets from a CSV upload.
// Each row is created in its own transaction via CsvRowImporter, so partial success is real.
// The response "42 created, 3 failed" means exactly that, not all or nothing.
// Bean Validation runs before each insert so bad input is reported cleanly instead of
// blowing up inside Hibernate.
@Service
public class TicketImportService {

  private final CsvRowImporter rowImporter;
  private final ProjectService projects;
  private final UserRepository users;
  private final AuditService audit;
  private final Validator validator;

  public TicketImportService(
      CsvRowImporter rowImporter,
      ProjectService projects,
      UserRepository users,
      AuditService audit,
      Validator validator) {
    this.rowImporter = rowImporter;
    this.projects = projects;
    this.users = users;
    this.audit = audit;
    this.validator = validator;
  }

  // Parse the CSV using RFC 4180 rules (quoted fields with commas inside survive) and try
  // to create one ticket per row. Returns counters and a per row error list.
  // Throws only for whole file problems: missing project or malformed CSV header.
  public ImportSummary importCsv(long projectId, MultipartFile file) throws IOException {
    projects.findActive(projectId);
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("CSV file is empty");
    }
    int created = 0;
    int failed = 0;
    List<ImportError> errors = new ArrayList<>();

    try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
      CSVParser parser;
      try {
        parser =
            CSVFormat.RFC4180
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build()
                .parse(reader);
      } catch (IOException | IllegalArgumentException ex) {
        throw new IllegalArgumentException("Malformed CSV file: " + ex.getMessage());
      }
      // One row at a time. Validate, create in its own tx, record the result.
      try (parser) {
        for (CSVRecord record : parser) {
          long rowNumber = record.getRecordNumber();
          try {
            CreateTicketRequest req = toRequest(projectId, record);
            Set<ConstraintViolation<CreateTicketRequest>> violations = validator.validate(req);
            if (!violations.isEmpty()) {
              String msg =
                  violations.stream()
                      .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                      .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                      .reduce((a, b) -> a + "; " + b)
                      .orElse("validation failed");
              throw new IllegalArgumentException(msg);
            }
            rowImporter.createOne(req);
            created++;
          } catch (IllegalArgumentException | IllegalStateException ex) {
            failed++;
            errors.add(new ImportError(rowNumber, ex.getMessage()));
          } catch (RuntimeException ex) {
            failed++;
            errors.add(new ImportError(rowNumber, "Unexpected error: " + ex.getMessage()));
          }
        }
      }
    }

    audit.recordCustom(
        "IMPORT_TICKETS", "PROJECT", projectId, null, Map.of("created", created, "failed", failed));
    return new ImportSummary(created, failed, errors);
  }

  private CreateTicketRequest toRequest(long projectId, CSVRecord r) {
    String title = get(r, "title");
    String description = get(r, "description");
    TicketStatus status = parseEnum(TicketStatus.class, get(r, "status"));
    TicketPriority priority = parseEnum(TicketPriority.class, get(r, "priority"));
    TicketType type = parseEnum(TicketType.class, get(r, "type"));
    Long assigneeId = resolveAssignee(r);
    return new CreateTicketRequest(
        title, description, status, priority, type, projectId, assigneeId, null);
  }

  // Accepts either a numeric assigneeId column or an assigneeUsername fallback for
  // human edited CSVs. Blank means "no explicit assignee", so auto assign kicks in later.
  private Long resolveAssignee(CSVRecord r) {
    String raw = get(r, "assigneeId");
    if (raw == null || raw.isBlank()) {
      String byName = get(r, "assigneeUsername");
      if (byName != null && !byName.isBlank()) {
        return users
            .findActiveByUsernameIgnoreCase(byName.trim())
            .map(u -> u.getId())
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown assignee username: " + byName));
      }
      return null;
    }
    try {
      return Long.valueOf(raw.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("assigneeId must be numeric: " + raw);
    }
  }

  private String get(CSVRecord r, String key) {
    if (!r.isMapped(key)) {
      return null;
    }
    String v = r.get(key);
    return v == null ? null : v;
  }

  private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(type.getSimpleName() + " is required");
    }
    try {
      return Enum.valueOf(type, value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid " + type.getSimpleName() + ": " + value);
    }
  }
}
