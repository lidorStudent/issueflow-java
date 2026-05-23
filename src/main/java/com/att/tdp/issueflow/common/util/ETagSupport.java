package com.att.tdp.issueflow.common.util;

import com.att.tdp.issueflow.common.error.ConflictException;
import org.springframework.http.HttpHeaders;

// Glue between Hibernate @Version and HTTP ETag / If-Match.
// GET returns a weak ETag like W/"5". On PATCH the client sends it back as If-Match, and we
// throw a 409 ConflictException if the version on disk has moved on.
public final class ETagSupport {

  private ETagSupport() {}

  // Weak ETag. Same version means same entity, but the JSON bytes may differ.
  public static String etag(long version) {
    return "W/\"" + version + "\"";
  }

  public static HttpHeaders headers(long version) {
    HttpHeaders h = new HttpHeaders();
    h.setETag(etag(version));
    return h;
  }

  // Pull the numeric version out of an If-Match header.
  // Accepts both strong ("5") and weak (W/"5") forms.
  // Returns null if the header is missing or unparseable. Callers treat null as
  // "no concurrency check requested".
  public static Long parseIfMatch(String header) {
    if (header == null || header.isBlank()) {
      return null;
    }
    String raw = header.trim();
    if (raw.startsWith("W/")) {
      raw = raw.substring(2);
    }
    if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
      raw = raw.substring(1, raw.length() - 1);
    }
    try {
      return Long.valueOf(raw);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  public static void verify(Long ifMatch, long currentVersion) {
    if (ifMatch != null && ifMatch != currentVersion) {
      throw new ConflictException(
          "CONCURRENT_MODIFICATION",
          "If-Match version " + ifMatch + " does not match current version " + currentVersion);
    }
  }
}
