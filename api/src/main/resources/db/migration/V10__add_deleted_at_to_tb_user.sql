ALTER TABLE tb_user
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_id_deleted_at
    ON tb_user (id, deleted_at);