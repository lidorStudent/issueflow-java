package com.att.tdp.issueflow.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.common.error.ConflictException;
import org.junit.jupiter.api.Test;

class ETagSupportTest {

  @Test
  void formatsWeakETag() {
    assertThat(ETagSupport.etag(7)).isEqualTo("W/\"7\"");
  }

  @Test
  void parsesWeakAndStrongETags() {
    assertThat(ETagSupport.parseIfMatch("W/\"42\"")).isEqualTo(42L);
    assertThat(ETagSupport.parseIfMatch("\"42\"")).isEqualTo(42L);
    assertThat(ETagSupport.parseIfMatch("42")).isEqualTo(42L);
  }

  @Test
  void parsesGarbageAsNull() {
    assertThat(ETagSupport.parseIfMatch(null)).isNull();
    assertThat(ETagSupport.parseIfMatch("")).isNull();
    assertThat(ETagSupport.parseIfMatch("not a number")).isNull();
  }

  @Test
  void verifyPassesWhenMatchingOrAbsent() {
    assertThatCode(() -> ETagSupport.verify(null, 1L)).doesNotThrowAnyException();
    assertThatCode(() -> ETagSupport.verify(5L, 5L)).doesNotThrowAnyException();
  }

  @Test
  void verifyThrowsOnMismatch() {
    assertThatThrownBy(() -> ETagSupport.verify(3L, 5L)).isInstanceOf(ConflictException.class);
  }
}
