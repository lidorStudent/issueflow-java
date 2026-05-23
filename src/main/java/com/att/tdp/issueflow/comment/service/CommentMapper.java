package com.att.tdp.issueflow.comment.service;

import com.att.tdp.issueflow.comment.dto.CommentResponse;
import com.att.tdp.issueflow.comment.dto.MentionedUser;
import com.att.tdp.issueflow.comment.entity.Comment;
import java.util.Comparator;
import java.util.List;

public final class CommentMapper {

  private CommentMapper() {}

  public static CommentResponse toDto(Comment c) {
    List<MentionedUser> mentions =
        c.getMentionedUsers().stream()
            .map(u -> new MentionedUser(u.getId(), u.getUsername(), u.getFullName()))
            .sorted(Comparator.comparing(MentionedUser::id))
            .toList();
    return new CommentResponse(
        c.getId(), c.getTicketId(), c.getAuthorId(), c.getContent(), mentions);
  }
}
