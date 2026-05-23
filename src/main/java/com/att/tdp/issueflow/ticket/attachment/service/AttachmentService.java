package com.att.tdp.issueflow.ticket.attachment.service;

import com.att.tdp.issueflow.audit.service.AuditService;
import com.att.tdp.issueflow.common.error.BusinessRuleException;
import com.att.tdp.issueflow.common.error.NotFoundException;
import com.att.tdp.issueflow.common.security.CurrentUser;
import com.att.tdp.issueflow.config.AttachmentProperties;
import com.att.tdp.issueflow.ticket.attachment.entity.Attachment;
import com.att.tdp.issueflow.ticket.attachment.repository.AttachmentRepository;
import com.att.tdp.issueflow.ticket.core.service.TicketService;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

// Upload, list and delete attachments on tickets.
// We sniff the first 32 bytes of the file to decide the MIME type. The client header is
// ignored because it is trivial to fake. The 10MB limit is also enforced by Spring multipart,
// having it here too is a second line of defence.
@Service
public class AttachmentService {

  private final AttachmentRepository attachmentRepository;
  private final TicketService ticketService;
  private final FileStorage fileStorage;
  private final AttachmentProperties properties;
  private final AuditService auditService;

  public AttachmentService(
      AttachmentRepository attachmentRepository,
      TicketService ticketService,
      FileStorage fileStorage,
      AttachmentProperties properties,
      AuditService auditService) {
    this.attachmentRepository = attachmentRepository;
    this.ticketService = ticketService;
    this.fileStorage = fileStorage;
    this.properties = properties;
    this.auditService = auditService;
  }

  // Save an uploaded file against a ticket. Three checks:
  //  1. not empty
  //  2. size under the configured max
  //  3. content sniffs to one of the allowed types
  // The storage layer hashes the bytes with SHA-256 so identical uploads dedupe on disk.
  @Transactional
  public Attachment upload(long ticketId, MultipartFile file) {
    ticketService.findActive(ticketId);
    if (file == null || file.isEmpty()) {
      throw new BusinessRuleException("UPLOAD_EMPTY", "Uploaded file is empty");
    }
    if (file.getSize() > properties.maxSizeBytes()) {
      throw new BusinessRuleException(
          "UPLOAD_TOO_LARGE",
          "File exceeds maximum size of " + properties.maxSizeBytes() + " bytes");
    }
    String detectedContentType = sniffContentType(file);
    if (detectedContentType == null
        || !properties.allowedMimeTypes().contains(detectedContentType)) {
      throw new BusinessRuleException(
          "UPLOAD_TYPE_REJECTED",
          "File content does not match any allowed type " + properties.allowedMimeTypes());
    }

    FileStorage.StoredFile stored;
    try (InputStream in = file.getInputStream()) {
      stored = fileStorage.store(in, file.getSize());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    Attachment attachment = new Attachment();
    attachment.setTicketId(ticketId);
    attachment.setFilename(
        file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
    attachment.setContentType(detectedContentType);
    attachment.setSizeBytes(file.getSize());
    attachment.setSha256(stored.sha256());
    attachment.setStoragePath(stored.relativePath());
    attachment.setUploadedBy(CurrentUser.getOrThrow().id());
    Attachment saved = attachmentRepository.save(attachment);
    auditService.recordCreate(
        "ATTACHMENT",
        saved.getId(),
        Map.of(
            "ticketId", ticketId,
            "filename", saved.getFilename(),
            "contentType", saved.getContentType(),
            "sizeBytes", saved.getSizeBytes()));
    return saved;
  }

  @Transactional(readOnly = true)
  public List<Attachment> listForTicket(long ticketId) {
    ticketService.findActive(ticketId);
    return attachmentRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
  }

  @Transactional(readOnly = true)
  public Attachment findById(long id) {
    return attachmentRepository.findById(id).orElseThrow(() -> NotFoundException.attachment(id));
  }

  @Transactional
  public void delete(long ticketId, long attachmentId) {
    Attachment attachment = findById(attachmentId);
    if (!attachment.getTicketId().equals(ticketId)) {
      throw NotFoundException.attachment(attachmentId);
    }
    attachmentRepository.delete(attachment);
    try {
      fileStorage.delete(attachment.getStoragePath());
    } catch (IOException ignored) {
      // file may already be missing; metadata removal is the source of truth
    }
    auditService.recordDelete(
        "ATTACHMENT",
        attachmentId,
        Map.of("ticketId", ticketId, "filename", attachment.getFilename()));
  }

  public FileStorage storage() {
    return fileStorage;
  }

  // Peek the first 32 bytes, enough for every signature MimeSniffer checks. The multipart
  // stream is consumable once but Spring lets us call getInputStream() again later when we
  // actually store the file, so this peek does not waste the upload.
  private static String sniffContentType(MultipartFile file) {
    byte[] head = new byte[32];
    int read;
    try (InputStream in = file.getInputStream()) {
      read = in.readNBytes(head, 0, head.length);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    if (read <= 0) {
      return null;
    }
    if (read < head.length) {
      byte[] trimmed = new byte[read];
      System.arraycopy(head, 0, trimmed, 0, read);
      return MimeSniffer.sniff(trimmed);
    }
    return MimeSniffer.sniff(head);
  }
}
