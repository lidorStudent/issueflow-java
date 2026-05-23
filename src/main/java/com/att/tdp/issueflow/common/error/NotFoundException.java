package com.att.tdp.issueflow.common.error;

public class NotFoundException extends RuntimeException {
  private final String code;

  public NotFoundException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }

  public static NotFoundException user(long id) {
    return new NotFoundException("USER_NOT_FOUND", "User " + id + " not found");
  }

  public static NotFoundException project(long id) {
    return new NotFoundException("PROJECT_NOT_FOUND", "Project " + id + " not found");
  }

  public static NotFoundException ticket(long id) {
    return new NotFoundException("TICKET_NOT_FOUND", "Ticket " + id + " not found");
  }

  public static NotFoundException comment(long id) {
    return new NotFoundException("COMMENT_NOT_FOUND", "Comment " + id + " not found");
  }

  public static NotFoundException attachment(long id) {
    return new NotFoundException("ATTACHMENT_NOT_FOUND", "Attachment " + id + " not found");
  }
}
