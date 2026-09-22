-- V14__create_password_resets.sql
--
-- Se guarda el hash SHA-256 del token, nunca el token: quien lea la tabla no
-- puede cambiar la contrasena de nadie. Una fila se consume una sola vez
-- (used_at) y caduca sola (expires_at).

CREATE TABLE password_resets (
    id           UUID PRIMARY KEY,
    user_id      UUID        NOT NULL,
    token_hash   VARCHAR(64) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    used_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_password_resets_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_resets_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_password_resets_user_id ON password_resets (user_id, requested_at);
