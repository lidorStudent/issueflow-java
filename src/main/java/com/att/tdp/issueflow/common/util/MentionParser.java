package com.att.tdp.issueflow.common.util;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Pulls @handle tokens out of comment bodies.
// The lookbehind makes sure we do not match an @ inside an email like foo@bar.com or in
// chains like @@user.
public final class MentionParser {

  // (?<![\w@]) means do not match if preceded by a word char or another @.
  // Then a literal @ followed by the handle: letters, digits, underscore, dot, dash. Max 64.
  private static final Pattern PATTERN = Pattern.compile("(?<![\\w@])@([A-Za-z0-9_.-]{1,64})");

  private MentionParser() {}

  // Returns the handles found in the text, lower cased and deduped.
  // LinkedHashSet keeps the original order of first appearance.
  public static Set<String> extractUsernames(String text) {
    if (text == null || text.isBlank()) {
      return Set.of();
    }
    Matcher m = PATTERN.matcher(text);
    Set<String> out = new LinkedHashSet<>();
    while (m.find()) {
      out.add(m.group(1).toLowerCase(Locale.ROOT));
    }
    return out;
  }
}
