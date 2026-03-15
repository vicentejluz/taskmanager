ALTER TABLE tb_verification_tokens
    ALTER COLUMN token SET DATA TYPE UUID USING token::uuid,
    ALTER COLUMN token SET DEFAULT uuidv7(),
    ALTER COLUMN token SET NOT NULL;

ALTER TABLE tb_refresh_tokens
    ADD COLUMN reuse_detected BOOLEAN NOT NULL DEFAULT FALSE;
