package com.att.tdp.issueflow.ticket.attachment.service;

// Tiny magic byte sniffer for the four MIME types we accept. Did not feel worth pulling in
// Tika for a fixed allow list. Plain text is checked by hand: all bytes must be printable
// ASCII or common whitespace.
public final class MimeSniffer {

  // Standard file format signatures.
  private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
  private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};

  private MimeSniffer() {}

  // Returns the detected MIME type for the sample, or null if it does not match any of
  // the four formats we accept. The sample should be at least 16 bytes.
  public static String sniff(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    if (startsWith(bytes, PNG_MAGIC)) {
      return "image/png";
    }
    if (startsWith(bytes, JPEG_MAGIC)) {
      return "image/jpeg";
    }
    if (startsWith(bytes, PDF_MAGIC)) {
      return "application/pdf";
    }
    if (looksLikePlainText(bytes)) {
      return "text/plain";
    }
    return null;
  }

  private static boolean startsWith(byte[] bytes, byte[] prefix) {
    if (bytes.length < prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if (bytes[i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }

  // Heuristic for text/plain. Every byte must be printable ASCII or tab, CR, LF.
  // Anything outside that range (control chars, high bytes) disqualifies the sample.
  private static boolean looksLikePlainText(byte[] bytes) {
    for (byte b : bytes) {
      int unsigned = b & 0xFF;
      boolean printable = unsigned >= 0x20 && unsigned < 0x7F;
      boolean whitespace = unsigned == '\t' || unsigned == '\r' || unsigned == '\n';
      if (!printable && !whitespace) {
        return false;
      }
    }
    return true;
  }
}
