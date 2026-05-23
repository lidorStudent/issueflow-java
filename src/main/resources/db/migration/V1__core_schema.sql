CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    full_name       VARCHAR(255) NOT NULL,
    role            VARCHAR(16)  NOT NULL CHECK (role IN ('ADMIN', 'DEVELOPER')),
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_created_at ON users (created_at);

CREATE TABLE projects (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    owner_id        BIGINT       NOT NULL REFERENCES users (id),
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_projects_owner ON projects (owner_id);
CREATE INDEX idx_projects_deleted_at ON projects (deleted_at);

CREATE TABLE tickets (
    id                 BIGSERIAL PRIMARY KEY,
    project_id         BIGINT       NOT NULL REFERENCES projects (id),
    title              VARCHAR(255) NOT NULL,
    description        TEXT,
    status             VARCHAR(16)  NOT NULL CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')),
    priority           VARCHAR(16)  NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    type               VARCHAR(16)  NOT NULL CHECK (type IN ('BUG', 'FEATURE', 'TECHNICAL')),
    assignee_id        BIGINT       REFERENCES users (id),
    due_date           TIMESTAMPTZ,
    is_overdue         BOOLEAN      NOT NULL DEFAULT FALSE,
    last_escalated_at  TIMESTAMPTZ,
    deleted_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version            BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_tickets_project ON tickets (project_id);
CREATE INDEX idx_tickets_assignee ON tickets (assignee_id);
CREATE INDEX idx_tickets_status ON tickets (status);
CREATE INDEX idx_tickets_deleted_at ON tickets (deleted_at);
CREATE INDEX idx_tickets_due_date ON tickets (due_date) WHERE due_date IS NOT NULL;

CREATE TABLE ticket_dependencies (
    ticket_id   BIGINT NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    blocker_id  BIGINT NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ticket_id, blocker_id),
    CHECK (ticket_id <> blocker_id)
);

CREATE INDEX idx_ticket_deps_blocker ON ticket_dependencies (blocker_id);

CREATE TABLE comments (
    id          BIGSERIAL PRIMARY KEY,
    ticket_id   BIGINT      NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    author_id   BIGINT      NOT NULL REFERENCES users (id),
    content     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version     BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_comments_ticket ON comments (ticket_id);
CREATE INDEX idx_comments_author ON comments (author_id);
CREATE INDEX idx_comments_created_at ON comments (created_at DESC);

CREATE TABLE comment_mentions (
    comment_id  BIGINT NOT NULL REFERENCES comments (id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (comment_id, user_id)
);

CREATE INDEX idx_comment_mentions_user ON comment_mentions (user_id);

CREATE TABLE attachments (
    id            BIGSERIAL PRIMARY KEY,
    ticket_id     BIGINT       NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    filename      VARCHAR(255) NOT NULL,
    content_type  VARCHAR(127) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    sha256        VARCHAR(64)  NOT NULL,
    storage_path  VARCHAR(512) NOT NULL,
    uploaded_by   BIGINT       NOT NULL REFERENCES users (id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_attachments_ticket ON attachments (ticket_id);
