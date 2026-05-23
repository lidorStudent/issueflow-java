package com.att.tdp.issueflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.comment.dto.CreateCommentRequest;
import com.att.tdp.issueflow.comment.dto.UpdateCommentRequest;
import com.att.tdp.issueflow.comment.entity.Comment;
import com.att.tdp.issueflow.comment.service.CommentService;
import com.att.tdp.issueflow.common.audit.AuditContext;
import com.att.tdp.issueflow.common.security.UserPrincipal;
import com.att.tdp.issueflow.project.dto.CreateProjectRequest;
import com.att.tdp.issueflow.project.entity.Project;
import com.att.tdp.issueflow.project.service.ProjectService;
import com.att.tdp.issueflow.support.IntegrationTest;
import com.att.tdp.issueflow.ticket.assignment.AutoAssignmentService;
import com.att.tdp.issueflow.ticket.core.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.core.dto.UpdateTicketRequest;
import com.att.tdp.issueflow.ticket.core.entity.Ticket;
import com.att.tdp.issueflow.ticket.core.entity.TicketPriority;
import com.att.tdp.issueflow.ticket.core.entity.TicketStatus;
import com.att.tdp.issueflow.ticket.core.entity.TicketType;
import com.att.tdp.issueflow.ticket.core.service.TicketService;
import com.att.tdp.issueflow.ticket.dependency.service.TicketDependencyService;
import com.att.tdp.issueflow.user.dto.CreateUserRequest;
import com.att.tdp.issueflow.user.entity.User;
import com.att.tdp.issueflow.user.entity.UserRole;
import com.att.tdp.issueflow.user.service.UserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.DockerClientFactory;

@IntegrationTest
@EnabledIf(value = "isDockerAvailable", disabledReason = "Docker daemon is not running")
class EndToEndFlowTest {

  @SuppressWarnings("unused")
  static boolean isDockerAvailable() {
    try {
      return DockerClientFactory.instance().isDockerAvailable();
    } catch (Throwable t) {
      return false;
    }
  }

  @Autowired UserService users;
  @Autowired ProjectService projects;
  @Autowired TicketService tickets;
  @Autowired TicketDependencyService deps;
  @Autowired CommentService comments;
  @Autowired AutoAssignmentService autoAssign;

  @Test
  void fullLifecycle() {
    loginAs(new UserPrincipal(1L, "system-test", "ADMIN"));

    User admin =
        users.create(
            new CreateUserRequest("admin", "admin@x.com", "Admin", UserRole.ADMIN, "secret123"));
    User dev1 =
        users.create(
            new CreateUserRequest(
                "dev1", "dev1@x.com", "Developer One", UserRole.DEVELOPER, "secret123"));
    User dev2 =
        users.create(
            new CreateUserRequest(
                "dev2", "dev2@x.com", "Developer Two", UserRole.DEVELOPER, "secret123"));

    Project project = projects.create(new CreateProjectRequest("Sample", "demo", admin.getId()));

    Ticket auto =
        tickets.create(
            new CreateTicketRequest(
                "Unassigned",
                "auto-assign me",
                TicketStatus.TODO,
                TicketPriority.MEDIUM,
                TicketType.BUG,
                project.getId(),
                null,
                null));
    assertThat(auto.getAssigneeId()).isEqualTo(dev1.getId());

    Ticket explicit =
        tickets.create(
            new CreateTicketRequest(
                "Pinned",
                "go to dev2",
                TicketStatus.TODO,
                TicketPriority.LOW,
                TicketType.FEATURE,
                project.getId(),
                dev2.getId(),
                null));
    assertThat(explicit.getAssigneeId()).isEqualTo(dev2.getId());

    Ticket inProgress =
        tickets.update(
            auto.getId(),
            new UpdateTicketRequest(null, null, TicketStatus.IN_PROGRESS, null, null, null),
            auto.getVersion());

    assertThatThrownBy(
            () ->
                tickets.update(
                    inProgress.getId(),
                    new UpdateTicketRequest(null, null, TicketStatus.TODO, null, null, null),
                    inProgress.getVersion()))
        .hasMessageContaining("backward");

    deps.add(explicit.getId(), auto.getId());
    Ticket explicitInProgress =
        tickets.update(
            explicit.getId(),
            new UpdateTicketRequest(null, null, TicketStatus.IN_PROGRESS, null, null, null),
            explicit.getVersion());
    Ticket explicitInReview =
        tickets.update(
            explicitInProgress.getId(),
            new UpdateTicketRequest(null, null, TicketStatus.IN_REVIEW, null, null, null),
            explicitInProgress.getVersion());
    assertThatThrownBy(
            () ->
                tickets.update(
                    explicitInReview.getId(),
                    new UpdateTicketRequest(null, null, TicketStatus.DONE, null, null, null),
                    explicitInReview.getVersion()))
        .hasMessageContaining("blocker");

    Comment commentWithDev1 =
        comments.create(
            auto.getId(), new CreateCommentRequest(admin.getId(), "Hey @dev1 please look at this"));
    var page = comments.mentionsForUser(dev1.getId(), 0, 10);
    assertThat(page.getTotalElements()).isEqualTo(1);

    // PDF 3.6: on comment update the mention list is re-evaluated. Removed mentions go away,
    // newly added ones are persisted.
    comments.update(
        auto.getId(),
        commentWithDev1.getId(),
        new UpdateCommentRequest("Hey @dev2 instead, dev1 is out today"),
        commentWithDev1.getVersion());
    assertThat(comments.mentionsForUser(dev1.getId(), 0, 10).getTotalElements()).isZero();
    assertThat(comments.mentionsForUser(dev2.getId(), 0, 10).getTotalElements()).isEqualTo(1);

    Ticket overdueDraft =
        tickets.create(
            new CreateTicketRequest(
                "Late",
                "past due",
                TicketStatus.TODO,
                TicketPriority.LOW,
                TicketType.BUG,
                project.getId(),
                dev1.getId(),
                Instant.now().minusSeconds(3600)));
    AuditContext.runAsSystem(() -> tickets.applyEscalation(overdueDraft.getId(), Instant.now()));
    Ticket reloaded = tickets.findActive(overdueDraft.getId());
    assertThat(reloaded.getPriority()).isEqualTo(TicketPriority.MEDIUM);

    // PDF 3.7: a CRITICAL ticket past its due date stays CRITICAL but flips isOverdue.
    // A second escalation pass on the same ticket should be a no-op (idempotent).
    Ticket criticalOverdue =
        tickets.create(
            new CreateTicketRequest(
                "Already critical",
                "still past due",
                TicketStatus.TODO,
                TicketPriority.CRITICAL,
                TicketType.BUG,
                project.getId(),
                dev1.getId(),
                Instant.now().minusSeconds(3600)));
    assertThat(criticalOverdue.isOverdue()).isFalse();

    AuditContext.runAsSystem(() -> tickets.applyEscalation(criticalOverdue.getId(), Instant.now()));
    Ticket criticalAfterFirst = tickets.findActive(criticalOverdue.getId());
    assertThat(criticalAfterFirst.getPriority()).isEqualTo(TicketPriority.CRITICAL);
    assertThat(criticalAfterFirst.isOverdue()).isTrue();

    AuditContext.runAsSystem(() -> tickets.applyEscalation(criticalOverdue.getId(), Instant.now()));
    Ticket criticalAfterSecond = tickets.findActive(criticalOverdue.getId());
    assertThat(criticalAfterSecond.getPriority()).isEqualTo(TicketPriority.CRITICAL);
    assertThat(criticalAfterSecond.isOverdue()).isTrue();

    var workload = autoAssign.workload(project.getId());
    assertThat(workload).extracting("username").contains("dev1", "dev2");

    projects.softDelete(project.getId());
    List<Project> deleted = projects.findAllDeleted();
    assertThat(deleted).extracting("id").contains(project.getId());

    projects.restore(project.getId());
    assertThat(projects.findActive(project.getId())).isNotNull();
  }

  private void loginAs(UserPrincipal principal) {
    var auth =
        new UsernamePasswordAuthenticationToken(
            principal, "", List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
