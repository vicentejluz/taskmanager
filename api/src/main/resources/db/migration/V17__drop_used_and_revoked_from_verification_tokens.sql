DROP INDEX IF EXISTS uq_user_token_type_active;

ALTER TABLE tb_verification_tokens
    DROP COLUMN IF EXISTS used,
    DROP COLUMN IF EXISTS revoked;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_token_type
    ON tb_verification_tokens(user_id, token_type);