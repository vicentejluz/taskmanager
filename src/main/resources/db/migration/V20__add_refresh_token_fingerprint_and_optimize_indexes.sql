ALTER TABLE IF EXISTS tb_refresh_tokens
    ADD COLUMN IF NOT EXISTS fingerprint VARCHAR(64);

DROP INDEX IF EXISTS idx_refresh_token_token_family_id;

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_token_family_id
    ON tb_refresh_tokens(user_id, token_family_id);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_revoked_at
    ON tb_refresh_tokens(user_id, revoked_at)
    WHERE revoked_at IS NULL;