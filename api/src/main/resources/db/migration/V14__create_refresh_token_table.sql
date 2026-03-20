CREATE TABLE IF NOT EXISTS tb_refresh_token(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id BIGINT NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY(user_id) REFERENCES tb_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_user_id_token ON tb_refresh_token(user_id, token);

ALTER TABLE tb_verification_token
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();

ALTER TABLE tb_verification_token
    DROP COLUMN version;

