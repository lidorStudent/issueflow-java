package com.att.tdp.issueflow.common.error;

public class UnauthorizedException extends RuntimeException {
  private final String code;

  public UnauthorizedException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
