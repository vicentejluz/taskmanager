CREATE INDEX idx_deleted_at ON tb_user (deleted_at);

CREATE TYPE token_type AS ENUM ('EMAIL_VERIFICATION', 'PASSWORD_RESET');

CREATE TABLE IF NOT EXISTS tb_verification_token(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id BIGINT NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    token_type token_type NOT NULL,
    CONSTRAINT fk_verification_token_user FOREIGN KEY(user_id) REFERENCES tb_user(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_user_token_type_active
    ON tb_verification_token(user_id, token_type)
    WHERE revoked = false;