package com.att.tdp.issueflow.ticket.core.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.common.error.BusinessRuleException;
import com.att.tdp.issueflow.ticket.core.entity.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StatusTransitionValidatorTest {

  @ParameterizedTest
  @CsvSource({
    "TODO,IN_PROGRESS",
    "IN_PROGRESS,IN_REVIEW",
    "IN_REVIEW,DONE",
    "TODO,TODO",
    "IN_PROGRESS,IN_PROGRESS"
  })
  void allowsForwardAndSameState(TicketStatus from, TicketStatus to) {
    assertThatCode(() -> StatusTransitionValidator.validate(from, to)).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @CsvSource({"IN_PROGRESS,TODO", "IN_REVIEW,TODO", "DONE,TODO", "IN_REVIEW,IN_PROGRESS"})
  void rejectsBackward(TicketStatus from, TicketStatus to) {
    assertThatThrownBy(() -> StatusTransitionValidator.validate(from, to))
        .isInstanceOf(BusinessRuleException.class);
  }

  @ParameterizedTest
  @CsvSource({"TODO,IN_REVIEW", "TODO,DONE", "IN_PROGRESS,DONE"})
  void allowsForwardSkips(TicketStatus from, TicketStatus to) {
    assertThatCode(() -> StatusTransitionValidator.validate(from, to)).doesNotThrowAnyException();
  }

  @Test
  void rejectsAnyTransitionAwayFromDone() {
    assertThatThrownBy(
            () -> StatusTransitionValidator.validate(TicketStatus.DONE, TicketStatus.IN_PROGRESS))
        .isInstanceOf(BusinessRuleException.class);
  }
}
