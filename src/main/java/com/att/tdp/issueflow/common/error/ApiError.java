package com.att.tdp.issueflow.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

// Standard error shape we return to clients.
// code is a stable machine readable id (like USERNAME_TAKEN).
// message is the human prose. details carries field level errors when validation fails.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    String code, String message, Map<String, Object> details, Instant timestamp) {
  public static ApiError of(String code, String message) {
    return new ApiError(code, message, null, Instant.now());
  }

  public static ApiError of(String code, String message, Map<String, Object> details) {
    return new ApiError(code, message, details, Instant.now());
  }
}
