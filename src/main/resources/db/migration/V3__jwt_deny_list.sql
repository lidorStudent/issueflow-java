CREATE TABLE jwt_deny_list (
    jti          VARCHAR(64) PRIMARY KEY,
    expires_at   TIMESTAMPTZ NOT NULL,
    revoked_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_jwt_deny_expires_at ON jwt_deny_list (expires_at);
