package com.att.tdp.issueflow.comment.service;

import com.att.tdp.issueflow.audit.service.AuditService;
import com.att.tdp.issueflow.comment.dto.CreateCommentRequest;
import com.att.tdp.issueflow.comment.dto.UpdateCommentRequest;
import com.att.tdp.issueflow.comment.entity.Comment;
import com.att.tdp.issueflow.comment.repository.CommentRepository;
import com.att.tdp.issueflow.common.error.NotFoundException;
import com.att.tdp.issueflow.common.util.ETagSupport;
import com.att.tdp.issueflow.ticket.core.service.TicketService;
import com.att.tdp.issueflow.user.entity.User;
import com.att.tdp.issueflow.user.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// CRUD for ticket comments plus the /users/{id}/mentions lookup.
// Any authenticated user can edit or delete any comment, matching the README contract.
// Two writers race-check with If-Match on the version, and mentions are recomputed from the
// body on every write so they stay in sync.
@Service
public class CommentService {

  private final CommentRepository commentRepository;
  private final TicketService ticketService;
  private final UserRepository userRepository;
  private final MentionService mentionService;
  private final AuditService auditService;

  public CommentService(
      CommentRepository commentRepository,
      TicketService ticketService,
      UserRepository userRepository,
      MentionService mentionService,
      AuditService auditService) {
    this.commentRepository = commentRepository;
    this.ticketService = ticketService;
    this.userRepository = userRepository;
    this.mentionService = mentionService;
    this.auditService = auditService;
  }

  @Transactional
  public Comment create(long ticketId, CreateCommentRequest request) {
    ticketService.findActive(ticketId);
    if (!userRepository.existsActiveById(request.authorId())) {
      throw NotFoundException.user(request.authorId());
    }
    Comment comment = new Comment();
    comment.setTicketId(ticketId);
    comment.setAuthorId(request.authorId());
    comment.setContent(request.content());
    comment.setMentionedUsers(mentionService.resolve(request.content()));
    Comment saved = commentRepository.save(comment);
    auditService.recordCreate(
        "COMMENT",
        saved.getId(),
        Map.of(
            "ticketId", ticketId,
            "authorId", saved.getAuthorId(),
            "mentionCount", saved.getMentionedUsers().size()));
    return saved;
  }

  @Transactional(readOnly = true)
  public List<Comment> findByTicket(long ticketId) {
    ticketService.findActive(ticketId);
    return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
  }

  @Transactional
  public Comment update(
      long ticketId, long commentId, UpdateCommentRequest request, Long ifMatchVersion) {
    Comment comment = loadInTicket(ticketId, commentId);
    ETagSupport.verify(ifMatchVersion, comment.getVersion());
    String previousContent = comment.getContent();
    comment.setContent(request.content());
    Set<User> newMentions = mentionService.resolve(request.content());
    comment.setMentionedUsers(new HashSet<>(newMentions));
    auditService.recordUpdate(
        "COMMENT",
        comment.getId(),
        Map.of("content", previousContent),
        Map.of("content", comment.getContent(), "mentionCount", newMentions.size()));
    return comment;
  }

  @Transactional
  public void delete(long ticketId, long commentId) {
    Comment comment = loadInTicket(ticketId, commentId);
    commentRepository.delete(comment);
    auditService.recordDelete("COMMENT", commentId, Map.of("ticketId", ticketId));
  }

  @Transactional(readOnly = true)
  public Page<Comment> mentionsForUser(long userId, int zeroBasedPage, int size) {
    if (!userRepository.existsActiveById(userId)) {
      throw NotFoundException.user(userId);
    }
    return commentRepository.findMentionsForUser(
        userId, PageRequest.of(Math.max(zeroBasedPage, 0), Math.min(Math.max(size, 1), 100)));
  }

  private Comment loadInTicket(long ticketId, long commentId) {
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> NotFoundException.comment(commentId));
    if (!comment.getTicketId().equals(ticketId)) {
      throw NotFoundException.comment(commentId);
    }
    return comment;
  }
}
