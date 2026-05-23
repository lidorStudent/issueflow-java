package com.att.tdp.issueflow.ticket.attachment.repository;

import com.att.tdp.issueflow.ticket.attachment.entity.Attachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

  List<Attachment> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
