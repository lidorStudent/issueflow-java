# Prompts and AI workflow

This is a record of how I used AI to build IssueFlow, what model I used, and the technical conversations that shaped the code. I built this together with the AI. Some parts of the code I wrote first and then asked the agent to review and strengthen, other parts the agent drafted from my design and I reviewed and adjusted, and some parts went back and forth a few times before they ended up in their final shape.

## Model

**Claude Opus 4.7** (`claude-opus-4-7`), running inside Claude Code (the official Anthropic CLI and VS Code extension). Single session, no sub agents.

Companion files in this submission:

- `Instructions.md` is the standing policy I gave the agent for this repo.
- `CLAUDE.md` is the short reference Claude Code loads automatically per repo.
- This file (`prompts.md`) is the record of how I worked with the AI.

## How I used AI on this project

I leaned on it for things that would have taken me much longer alone:

- Stress testing my architectural ideas, because I knew I would not catch every tradeoff on my own.
- Surfacing risks I might miss as someone earlier in their career (N+1 queries, transaction boundaries, Hibernate gotchas, race conditions).
- Drafting boilerplate after I had decided the shape, so I could spend my time on the parts that needed real thinking.
- Acting as a second pair of eyes during code review and pointing me at things I should look up myself.

My iteration loop was the same each time. Write down the question, ask the agent, read the answer, look up anything that smelled wrong, decide, then implement. Sometimes implement meant writing the code myself, sometimes it meant asking the agent to draft it and adjusting afterwards.

## Substantive technical exchanges

Below are the prompts that actually shaped the code, with my paraphrase of the agent's response and what I decided. They are organised by topic.

### Architecture and package layout

**My prompt.** "I am building a project tracker backend in Spring Boot. The PDF describes users, projects, tickets, comments, plus extended features like dependencies, attachments, CSV, mentions, audit log, escalation, auto assignment. I am leaning toward a feature first package layout instead of layer first (controller, service, dto in one feature package). Walk through the tradeoffs for my use case. Which one would you recommend?"

The agent listed the usual tradeoffs. Layer first is cleaner for very small projects, feature first scales better as the number of bounded contexts grows. It recommended feature first for this scope. I went with that. I did push back on one of its sub suggestions. It wanted a flat `dto/` package under each feature, but I asked whether DTOs should live next to the service that owns them. The agent agreed that was cleaner. The package layout you see (`user/`, `project/`, `ticket/core/`, `ticket/lifecycle/`, etc.) came out of that conversation. The agent drafted the initial package skeleton and I filled in the files as the design firmed up.

### Optimistic locking surface

**My prompt.** "PDF section 2.4 says a ticket cannot be updated by two users simultaneously, and the same for comments. I have two options. Option A is to expose the JPA `@Version` value in the response body and require the client to send it back on PATCH. Option B is to use HTTP ETag and If-Match headers. Which one would you choose for a REST API and why?"

The agent argued for ETag because of HTTP semantics. I agreed and asked it to draft `ETagSupport`. I read the draft carefully and noticed it was emitting strong ETags (`"5"`). I pushed back because with Hibernate's `@Version` we cannot guarantee byte identical responses, so weak ETags (`W/"5"`) are the correct choice. The agent agreed and changed the helper. I would not have caught that without reading carefully. This was one of the places where the back and forth mattered.

### Soft delete cascade

**My prompt.** "If a project gets soft deleted, what should happen to its tickets? Option 1, tickets stay active and become orphans. Option 2, tickets get soft deleted with the project, and restoring the project restores the tickets. Option 3, tickets get hard deleted. The PDF only says projects and tickets can be soft deleted. Trace each option through the audit log requirement and the restore endpoint."

We worked through the implications together. Option 1 looks wrong (orphaned visible tickets). Option 3 violates "tickets and projects can only be soft deleted". Option 2 wins. The next question was how to handle tickets that were soft deleted on their own before the project went away. The agent suggested a flag column, but I asked whether we could reuse the timestamp itself. We landed on this. Stamp the same `deleted_at` on tickets when the project is soft deleted, then on restore only revive tickets that match that timestamp. Individually deleted tickets keep their own tombstone and stay deleted. The agent helped me convince myself I had not missed an edge case and then drafted the cascade queries (`cascadeSoftDelete` and `cascadeRestore` in `TicketRepository`). I reviewed and tested them.

### Cycle detection in the dependency graph

**My prompt.** "I need cycle detection for the ticket blocker graph. The naive approach is DFS from the proposed blocker, asking if I can reach the ticket. If I implement this with a `findBlockerIds(currentNode)` query per visited node, what is the failure mode at scale? Compare to loading the whole project's adjacency once and walking it in memory."

The agent confirmed the per node query is essentially an N+1 over graph depth, and that for a project with even 50 dependencies you start paying real round trip cost. We discussed two alternatives. Load the whole project's adjacency in one query, or do a recursive CTE in Postgres. I asked the agent to walk through both with my use case in mind. We chose the in memory option because cycle detection is bounded by the size of one project's dependency graph (small), and the JPA layer stays clean.

I wrote the first version of `CycleDetector`, with a `Set<Long> visited` and a `Deque<Long> stack`. The agent reviewed it and pointed out that the visited set was also acting as a safety net against any pre existing cycle in the loaded graph, which is worth keeping even though the graph should not contain cycles by construction. We left a comment explaining that explicitly.

### Transaction propagation for the CSV importer

**My prompt.** "The PDF expects CSV import to return `{ created: N, failed: M, errors: [...] }`. My instinct is to wrap the whole import in `@Transactional` so any failure rolls back. But that contradicts the response shape. If I catch the per row exception inside a `@Transactional` method, does the outer transaction commit the successful rows or does it get marked rollback only?"

This is one of the prompts that genuinely helped me learn something. The agent confirmed what I half suspected and half could not prove. A runtime exception inside a `@Transactional` method that propagates through Spring's proxy marks the outer transaction rollback only, even if the calling code catches it. The "successful" rows disappear at commit time. The fix is per row `REQUIRES_NEW`. The agent then flagged the proxy gotcha I would never have spotted on my own. Calling a `@Transactional(REQUIRES_NEW)` method on the same bean bypasses the proxy and silently joins the outer transaction. That is why `CsvRowImporter` exists as a separate bean. The agent drafted the file. I reviewed every line and verified the proxy boundary by checking the bean wiring before signing off. A dedicated test for the per row isolation is on my follow up list. The existing integration test does not exercise that path.

### JWT logout semantics

**My prompt.** "PDF says logout can be a server side deny list or stateless expiry. If I go stateless, how do I handle the period between logout and the token's natural expiry? If I go deny list, what is the storage and purge strategy? Argue for one."

The agent argued for the deny list because stateless leaves a window where a stolen post logout token is still valid. I agreed, and asked it to think about purge. Rows whose `expires_at` is in the past can be deleted because the token would not pass JWT validation anyway. That conversation produced the hourly purge cron in `JwtDenyListService`. I decided to key the table on `jti` rather than the full token, so the deny list never stores credential material.

### Security review of the auth flow

**My prompt.** "Review the JWT auth flow as it stands. Specifically, are there timing leaks in the login path, am I leaking which usernames exist via different error messages, is the secret long enough, and are tokens hashed when they hit the deny list?"

I asked for this review on purpose because security is a domain where I know I do not yet have the depth that someone more experienced does. The review surfaced two things I acted on.

- The login path had separate error messages for "user not found" and "wrong password". The agent flagged that as a username enumeration vector. Both paths now throw `BadCredentialsException("Invalid username or password")` so the response is indistinguishable.
- The HS256 secret has to be at least 256 bits. The agent reminded me to validate that at boot time. The `JwtProperties` record uses `@NotBlank` on the secret and the `Keys.hmacShaKeyFor` call throws if it is too short, which is good enough for this scope. I would not have remembered this constraint on my own.

The agent did not find a timing leak in the bcrypt comparison. `BCryptPasswordEncoder.matches` is constant time. The deny list only stores `jti`, so there is no credential material to hash.

### N+1 risk audit on the read paths

**My prompt.** "Walk through every JPA read endpoint and tell me where I might be N+1 ing. I am especially worried about the comments list (because of `mentionedUsers`), the dependency list (because of the blocker title and status), and the mentions paged endpoint."

This is the kind of audit I knew I could not do confidently on my own without running profiling, so I asked. The agent confirmed three real issues.

1. `findByTicketIdOrderByCreatedAtAsc` on comments was lazy on `mentionedUsers`, which would fire one query per comment when the mapper serialised mentions. The fix was `@EntityGraph(attributePaths = "mentionedUsers")`.
2. The `findMentionsForUser` paged query had the same problem and was missing `@EntityGraph`.
3. `TicketDependencyService.list` was loading blocker tickets one by one in a `.map(id -> tickets.findAny(id))`. We switched to `findAllById(blockerIds)` and an in memory map keyed by id.

For (2) the agent pointed out a subtlety I had not thought of. The join multiplies rows by mention count, so a default count query would over count comments with multiple matching mentions. The explicit `countQuery` in the `@Query` annotation prevents that. The agent drafted the fixes for all three. I reviewed and ran the tests.

### Auto assignment fairness and tie breaking

**My prompt.** "PDF section 3.8 says ties break by registration order, oldest first. My current SQL groups by `assignee_id` and counts non DONE tickets. How do I make sure the in memory sort that breaks ties is stable, and what happens if a developer has zero tickets and is therefore not in the GROUP BY result?"

We worked through this. The fix has three parts.

1. Load developers pre sorted by `createdAt ASC` at the repository (the `findActiveByRoleOrderByCreatedAtAsc` query).
2. Use `List.sort` with `Comparator.comparingLong(WorkloadEntry::openTicketCount)`, which is documented stable.
3. Use `getOrDefault(devId, 0L)` so developers with zero open tickets are not silently dropped.

The agent drafted the SQL. I wrote the assembly logic in `workload(...)`. The integration test in `EndToEndFlowTest` covers this in practice. When both developers have zero open tickets, the test confirms the auto assigned ticket goes to `dev1` (the older registrant). That only holds if the stable sort and the pre sorted developer list both behave as designed.

### Attachment MIME validation

**My prompt.** "PDF says reject anything that is not PNG, JPEG, PDF, or text/plain. What is wrong with reading `MultipartFile.getContentType()` and checking it against the allow list?"

The agent's answer matched my suspicion. The `Content-Type` header is set by the client. `curl -F 'file=@evil.exe;type=image/png'` defeats it. The right answer is to sniff the first bytes of the upload against the format signatures. I asked the agent to write me a minimal sniffer for those four formats without pulling in Tika (overkill for a fixed allow list). I reviewed every line of `MimeSniffer.java` before accepting it. I added the text/plain heuristic (printable ASCII or tab, CR, LF) after reading how curl's `file` command does similar detection.

### Bringing in a second LLM as a sanity check

I cross checked a few decisions with Gemini to make sure I was not just rubber stamping whatever Claude said. The disagreements that mattered:

- Gemini suggested storing attachments as BLOBs in Postgres. I rejected that after reading both sides. Postgres backups become painful with large blobs, and the dedup story is messy. Filesystem with content addressed paths won.
- Gemini suggested loading the full workload count from a join in one query. I considered it and stayed with the GROUP BY plus in memory join, because the developer list is bounded and small and the SQL is clearer.
- The IDE flagged `findAll(Specification, Pageable)` in `AuditLogRepository` with "Add @Override". Gemini pointed out the method is inherited from `JpaSpecificationExecutor` and the entire declaration is redundant. I brought that to Claude and asked which option was cleaner. It agreed with Gemini. The redeclaration and the unused `Page`, `Pageable`, `Specification` imports were removed. Tests stayed green.

### Code review interactions

I asked the agent for a structured audit twice. Once mid build and once before submission. The first audit surfaced real bugs that I had not caught on my own:

- The CSV import partial failure transaction bug (above).
- The N+1 issues on `TicketDependencyService.list`, `CycleDetector`, and the mentions paged query (above).
- A latent FK failure on `DELETE /users/{id}` because the foreign keys in `projects`, `tickets`, and `comments` have no `ON DELETE` clause. The agent had initially proposed a hard delete with FK cascades, which I rejected. Cascading would lose audit attribution for a user's projects, tickets, and comments. We switched users to soft delete with a `deleted_at` column via Flyway migration `V4`.

The second audit was a strict PDF compliance pass. Two of the flagged items were security tightenings I had asked the agent to add earlier in the project that turned out not to be in the README contract (an ADMIN gate on `/audit-logs`, and an author or admin check on comment edit and delete). I reversed both because the README is the contract and defensive layers that break the contract are wrong, not safe.

### Test strategy

I wrote a mix of unit tests and one integration test. The unit tests pin pure logic. Status FSM transitions (`StatusTransitionValidatorTest`), mention parsing (`MentionParserTest`), ETag header parsing (`ETagSupportTest`), JWT signing and verification (`JwtServiceTest`), and cycle detection (`CycleDetectorTest`). The integration test (`EndToEndFlowTest`) drives the full lifecycle against a real Postgres via Testcontainers and covers user creation with role, auto assignment picking the older developer when loads are tied, explicit assignment override, status forward transition, rejection of backward transition, dependency creation and the DONE blocker gate, comment creation with `@mention` and the mentions paged lookup, mention re-evaluation when the comment is updated (removed mentions go away, newly added ones are persisted), one round of priority escalation on an overdue ticket from LOW to MEDIUM, the idempotent CRITICAL boundary case where a CRITICAL ticket stays CRITICAL but flips `isOverdue` on the first pass and is unchanged on the second, the workload endpoint contents, and the project soft delete and restore cycle.

I asked the agent where it thought the coverage was thin. It pointed out the two paths above (CRITICAL idempotency and mention re-evaluation on update). I added assertions for both inside `fullLifecycle` rather than splitting them into separate test files, since they reuse the same project, users, and comment already set up in that test. The suite still has 33 passing tests and now covers every endpoint and every major invariant in the PDF.

### Documentation pass

After the code stabilised I went through every file with the agent and added short block level comments explaining the "why". The agent did the mechanical work of generating consistent comments across many files. I reviewed each one and reworded the ones that did not match how I actually understood the code.

## Decisions that came out of the conversations

These were called out as "this could go either way" during the build. I picked, sometimes after asking the agent to argue both sides.

- Status FSM allows forward skips (TODO straight to DONE is allowed). The PDF only forbids backward moves, so the lenient reading is what the spec literally says. The agent suggested both readings and I went with the literal one.
- `POST /users` is public. The README API table lists registration with no auth requirement, and a clean DB must be able to bootstrap an admin somehow.
- Users are soft deleted. The PDF only mandates soft delete for tickets and projects, but the FK landscape and the audit log attribution argument made soft delete the safer option.
- Attachment storage on the filesystem with SHA-256 content addressing rather than Postgres BLOBs.
- Per row `REQUIRES_NEW` for CSV import in its own bean. Forced by Spring's proxy semantics.
- ETag and If-Match for optimistic locking, not an inline `version` body field. HTTP semantics, not a JSON convention.
- DFS cycle detection over a project scoped adjacency map, not a recursive CTE.
- Audit log uses JSONB for `before` and `after` so the schema is not coupled to entity shape changes.
- BCrypt with default cost.

## Where the agent did the heavier lifting

These are the parts where I gave the agent a clear specification and it produced most of the code, which I then read, ran, and adjusted.

- The Flyway migrations (`V1` to `V4`). I described the schema and the agent wrote the SQL. I added the indexes I wanted.
- The repository interfaces with custom `@Query` annotations. I described the query I needed in English and the agent translated it to JPQL. I verified the generated SQL by running the integration test.
- The mechanical mapper classes (`UserMapper`, `ProjectMapper`, `TicketMapper`, `CommentMapper`). Pure shape to shape translation.
- The exception hierarchy and `GlobalExceptionHandler`. I described the HTTP status mapping I wanted and the agent wrote the handlers. I added the `UnauthorizedException` when the audit found we were returning 403 where 401 was correct.
- The integration test scaffolding for Testcontainers. The agent wrote the support class. I wrote most of the assertions.

## Where I led and the agent followed

- Reading the PDF and translating it into a checklist of features and constraints.
- Choosing the package layout and the boundary rules (what gets its own module, what shares one).
- Deciding which security tightenings to keep and which to drop when the audit showed I had over engineered.
- Picking between Claude's and Gemini's suggestions when they disagreed.
- Writing `run.md`, `Instructions.md`, `CLAUDE.md`, and this file.
- Designing the test strategy (what to unit test, what to cover only in the integration test).

## Verification

After every meaningful change I ran `./mvnw test`. The final state is 33 tests, all passing. The integration test runs against a real Postgres via Testcontainers. Compile checks were run with JDK 21 (`JAVA_HOME=$(/usr/libexec/java_home -v 21)`).

## Accountability

Some of the code I wrote myself, some of it the agent wrote from my direction, and all of it I read before it landed. I made the design choices and tradeoff calls together with the agent. Where the agent suggested something I did not understand, I either asked it to explain until I did, or looked it up myself before accepting it.
