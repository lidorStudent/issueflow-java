# IssueFlow agent guide

This file is loaded automatically by Claude Code. It is the short, day to day reference. The fuller policy is in `Instructions.md` and the conversation record is in `prompts.md`.

## Quick context

- Spring Boot 3.4 on Java 21, Postgres via Docker compose.
- JPA + Hibernate, Flyway migrations under `src/main/resources/db/migration/`.
- JWT auth with a server side deny list for logout.
- ETag and If-Match for optimistic locking on PATCH endpoints.

## Layout

```
src/main/java/com/att/tdp/issueflow/
  IssueFlowApplication.java        skeleton, do not modify
  config/                          Spring config and properties beans
  common/
    error/                         GlobalExceptionHandler and typed exceptions
    security/                      JwtService, JwtAuthFilter, deny list, CurrentUser
    audit/                         AuditContext for SYSTEM actor tagging
    util/                          ETagSupport, MentionParser
  user/                            Users API + bcrypt
  auth/                            /auth/login, /logout, /me
  project/                         Projects API + soft delete
  ticket/
    core/                          Tickets API, FSM, optimistic locking
    lifecycle/                     EscalationScheduler
    assignment/                    AutoAssignmentService + /workload
    dependency/                    Dependencies + cycle detector + DONE gate
    attachment/                    File upload, magic byte sniff, SHA-256 dedup
    csv/                           Export and import (RFC 4180)
  comment/                         Comments + @mention parsing
  audit/                           AuditLog entity + /audit-logs
```

## Standing rules

1. Don't touch skeleton files: `README.md`, `IssueFlowApplication.java`, `compose.yml`, `mvnw*`, `schema.sql`, `data.sql`, existing tests. New code goes in new files.
2. PDF wins over README on conflict, README wins over personal preference.
3. Constructor injection only. Records for DTOs. Bean Validation annotations on request DTOs.
4. Comments are short `//` lines, explain why. No Javadoc tags in service code.
5. Run `./mvnw test` after non trivial changes. Do not mark a task done on red.

## Things that already burned me

- Don't add ADMIN gates that aren't in the README. We had to undo them.
- Don't restrict comment edit or delete to the author. Same reason.
- `JpaSpecificationExecutor.findAll(Specification, Pageable)` is inherited. Do not redeclare it.
- Per row REQUIRES_NEW for CSV import must live in a separate bean. Calling `@Transactional` on the same bean bypasses the proxy.
- Lazy `@ManyToMany` on `Comment.mentionedUsers` needs `@EntityGraph` on the paged mentions query or it N+1s.

## When in doubt

Ask before making a design call. The list of decisions I have already made:
- Status FSM allows forward skips (literal PDF reading).
- `POST /users` is public per the README API table.
- Users are soft deleted, not hard deleted.
- MIME validation sniffs the first 32 bytes, the client header is ignored.
- The escalation scheduler logs and continues on per ticket failures.
