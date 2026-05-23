package com.att.tdp.issueflow.comment.repository;

import com.att.tdp.issueflow.comment.entity.Comment;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Comment persistence. Both reads use @EntityGraph to eagerly fetch mentionedUsers.
// Without it, serializing mentions per comment would fire one query per comment (N+1).
public interface CommentRepository extends JpaRepository<Comment, Long> {

  @EntityGraph(attributePaths = "mentionedUsers")
  List<Comment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

  // We need an explicit count query because the join multiplies rows by mention count.
  // The default count would double count comments that have several matching mentions.
  @EntityGraph(attributePaths = "mentionedUsers")
  @Query(
      value =
          """
            SELECT c FROM Comment c JOIN c.mentionedUsers u
            WHERE u.id = :userId
            ORDER BY c.createdAt DESC
            """,
      countQuery =
          """
            SELECT COUNT(c) FROM Comment c JOIN c.mentionedUsers u
            WHERE u.id = :userId
            """)
  Page<Comment> findMentionsForUser(@Param("userId") Long userId, Pageable pageable);
}
