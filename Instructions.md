# Instructions for the AI agent on this project

These are the standing instructions I used while building IssueFlow with Claude Code (Claude Opus 4.7). They sit alongside `prompts.md` (which shows the actual conversations) and `CLAUDE.md` (which the agent picks up automatically per repo). The split is intentional: this file is the high level "what to do and what not to do", `CLAUDE.md` is the day to day style guide, `prompts.md` is the record of real exchanges.

## Source of truth

1. The Requirements PDF is the master spec. When the PDF and `README.md` disagree, the PDF wins.
2. The `README.md` API table is the implementation contract for endpoints, request and response shapes.
3. The skeleton files (`README.md`, `IssueFlowApplication.java`, `compose.yml`, `mvnw*`, `schema.sql`, `data.sql`, existing tests) are not to be modified. Anything new goes into a new file. Modifications to `pom.xml` and `application.yaml` are allowed because the requirements cannot be implemented otherwise, but each change must be tied to a specific PDF section.

## How I want the agent to behave

- No silent assumptions. If the spec is ambiguous, ask. Examples that came up: how to bootstrap an admin, what to do when the README example body omits the password field, whether the status FSM should allow level skips. These are decisions I make, not the agent.
- Prefer simple, idiomatic Spring Boot. Constructor injection, no field injection. Records for DTOs. Static utility classes for pure helpers (mappers, validators).
- One file, one responsibility. Don't pile cross cutting concerns into a "Util" bag.
- Tests are not optional. Every non trivial behaviour gets at least one unit or integration test. Use Testcontainers for the integration test so it talks to a real Postgres.
- After every meaningful change, run `./mvnw test`. Do not declare a task done on a failing build.

## Code style

- Google Java Format, 2 space indent. Don't fight the formatter.
- Naming: full words, no abbreviations. `ticketRepository`, not `repo`. `projectService`, not `projects`.
- Method names start with a verb, classes are nouns.
- Comments are `//` lines, short, explain the why not the what. Skip Javadoc tags like `@link` and `@code` in regular comments, those belong on real public API surfaces.
- No emoji.

## Boundaries the agent must respect

- Never delete or modify skeleton files (listed above).
- Never bypass git hooks, never force push, never amend a published commit without me telling you to.
- When the agent wants to change shared state (push, install, db reset), ask first.
- Treat the audit log as append only. No service should ever UPDATE or DELETE an audit row.

## Decision policy when stuck

- PDF says X, README says Y, agent has opinion Z. The PDF wins unless I overrule it for a specific item.
- When two readings are both defensible, surface the trade off and let me choose. Do not silently pick.
- Security tightening beyond the spec needs a justification I sign off on. We pulled extra ADMIN gates and an author check on comments back out because they weren't in the contract.

## What good output looks like

- A diff I can read in under five minutes.
- A passing test that pins the behaviour.
- A short note in `run.md` if the behaviour is user facing.
- No drive by refactors. If something else looks wrong, surface it, don't quietly change it.
