ALTER TABLE IF EXISTS tb_refresh_tokens
    ADD COLUMN IF NOT EXISTS token_family_id UUID NOT NULL,
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP WITH TIME ZONE,
    DROP COLUMN IF EXISTS revoked;

CREATE INDEX IF NOT EXISTS idx_refresh_token_token_family_id ON tb_refresh_tokens(token_family_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON tb_refresh_tokens(expires_at);