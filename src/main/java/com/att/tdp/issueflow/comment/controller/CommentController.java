package com.att.tdp.issueflow.comment.controller;

import com.att.tdp.issueflow.comment.dto.CommentResponse;
import com.att.tdp.issueflow.comment.dto.CreateCommentRequest;
import com.att.tdp.issueflow.comment.dto.UpdateCommentRequest;
import com.att.tdp.issueflow.comment.entity.Comment;
import com.att.tdp.issueflow.comment.service.CommentMapper;
import com.att.tdp.issueflow.comment.service.CommentService;
import com.att.tdp.issueflow.common.util.ETagSupport;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Comment REST endpoints, nested under their ticket. PATCH uses ETag and If-Match, so two
// users editing the same comment race-check on the @Version field.
@RestController
@RequestMapping("/tickets/{ticketId}/comments")
public class CommentController {

  private final CommentService service;

  public CommentController(CommentService service) {
    this.service = service;
  }

  @GetMapping
  public List<CommentResponse> list(@PathVariable long ticketId) {
    return service.findByTicket(ticketId).stream().map(CommentMapper::toDto).toList();
  }

  @PostMapping
  public CommentResponse create(
      @PathVariable long ticketId, @Valid @RequestBody CreateCommentRequest req) {
    return CommentMapper.toDto(service.create(ticketId, req));
  }

  @PatchMapping("/{commentId}")
  public ResponseEntity<CommentResponse> update(
      @PathVariable long ticketId,
      @PathVariable long commentId,
      @Valid @RequestBody UpdateCommentRequest req,
      @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
    Long version = ETagSupport.parseIfMatch(ifMatch);
    Comment c = service.update(ticketId, commentId, req, version);
    return ResponseEntity.ok()
        .headers(ETagSupport.headers(c.getVersion()))
        .body(CommentMapper.toDto(c));
  }

  @DeleteMapping("/{commentId}")
  public void delete(@PathVariable long ticketId, @PathVariable long commentId) {
    service.delete(ticketId, commentId);
  }
}
