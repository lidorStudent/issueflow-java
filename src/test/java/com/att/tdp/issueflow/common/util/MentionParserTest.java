package com.att.tdp.issueflow.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MentionParserTest {

  @Test
  void extractsBasicMentions() {
    assertThat(MentionParser.extractUsernames("hi @jdoe and @alice"))
        .containsExactly("jdoe", "alice");
  }

  @Test
  void isCaseInsensitive() {
    assertThat(MentionParser.extractUsernames("hello @JDoe and @JDOE")).containsExactly("jdoe");
  }

  @Test
  void ignoresEmailAddresses() {
    assertThat(MentionParser.extractUsernames("ping jdoe@example.com")).isEmpty();
  }

  @Test
  void handlesNullAndBlank() {
    assertThat(MentionParser.extractUsernames(null)).isEmpty();
    assertThat(MentionParser.extractUsernames("")).isEmpty();
    assertThat(MentionParser.extractUsernames("   ")).isEmpty();
  }

  @Test
  void allowsDotsHyphensUnderscores() {
    assertThat(MentionParser.extractUsernames("@a.b-c_d hello")).containsExactly("a.b-c_d");
  }

  @Test
  void deduplicates() {
    assertThat(MentionParser.extractUsernames("@x and @x and @x")).containsExactly("x");
  }
}
