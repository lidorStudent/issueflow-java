package com.att.tdp.issueflow.comment.controller;

import com.att.tdp.issueflow.comment.dto.CommentResponse;
import com.att.tdp.issueflow.comment.dto.MentionsPageResponse;
import com.att.tdp.issueflow.comment.entity.Comment;
import com.att.tdp.issueflow.comment.service.CommentMapper;
import com.att.tdp.issueflow.comment.service.CommentService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// "Who tagged me" endpoint. Paged, newest first.
// Page numbers are 1 based on the wire to match the documented contract. We translate to
// Spring's 0 based index internally.
@RestController
@RequestMapping("/users/{userId}/mentions")
public class MentionController {

  private final CommentService service;

  public MentionController(CommentService service) {
    this.service = service;
  }

  @GetMapping
  public MentionsPageResponse mentions(
      @PathVariable long userId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    int zeroBasedPageIndex = Math.max(page - 1, 0);
    Page<Comment> mentionsPage = service.mentionsForUser(userId, zeroBasedPageIndex, pageSize);
    List<CommentResponse> data = mentionsPage.stream().map(CommentMapper::toDto).toList();
    return new MentionsPageResponse(data, mentionsPage.getTotalElements(), page);
  }
}
