package com.att.tdp.issueflow.ticket.attachment.controller;

import com.att.tdp.issueflow.ticket.attachment.dto.AttachmentResponse;
import com.att.tdp.issueflow.ticket.attachment.entity.Attachment;
import com.att.tdp.issueflow.ticket.attachment.service.AttachmentService;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// Attachment upload, list, download and delete.
// Downloads stream via FileSystemResource so we do not buffer 10MB blobs in memory.
@RestController
public class AttachmentController {

  private final AttachmentService service;

  public AttachmentController(AttachmentService service) {
    this.service = service;
  }

  @PostMapping(
      value = "/tickets/{ticketId}/attachments",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public AttachmentResponse upload(
      @PathVariable long ticketId, @RequestParam("file") MultipartFile file) {
    Attachment a = service.upload(ticketId, file);
    return toDto(a);
  }

  @GetMapping("/tickets/{ticketId}/attachments")
  public List<AttachmentResponse> list(@PathVariable long ticketId) {
    return service.listForTicket(ticketId).stream().map(AttachmentController::toDto).toList();
  }

  @GetMapping("/attachments/{attachmentId}")
  public ResponseEntity<Resource> download(@PathVariable long attachmentId) {
    Attachment a = service.findById(attachmentId);
    FileSystemResource resource =
        new FileSystemResource(service.storage().resolve(a.getStoragePath()));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(a.getContentType()))
        .contentLength(a.getSizeBytes())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(a.getFilename()).build().toString())
        .body(resource);
  }

  @DeleteMapping("/tickets/{ticketId}/attachments/{attachmentId}")
  public void delete(@PathVariable long ticketId, @PathVariable long attachmentId) {
    service.delete(ticketId, attachmentId);
  }

  private static AttachmentResponse toDto(Attachment a) {
    return new AttachmentResponse(
        a.getId(),
        a.getTicketId(),
        a.getFilename(),
        a.getContentType(),
        a.getSizeBytes(),
        a.getUploadedBy());
  }
}
