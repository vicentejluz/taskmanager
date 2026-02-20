ALTER TABLE tb_task
    DROP CONSTRAINT fk_task_user;

ALTER TABLE tb_role
    DROP CONSTRAINT fk_role_user;

ALTER TABLE tb_task
    ADD CONSTRAINT fk_task_user
        FOREIGN KEY (user_id)
            REFERENCES tb_user(id)
            ON DELETE CASCADE;

ALTER TABLE tb_role
    ADD CONSTRAINT fk_role_user
        FOREIGN KEY (user_id)
            REFERENCES tb_user(id)
            ON DELETE CASCADE;

CREATE INDEX idx_user_enabled_updated_at
    ON tb_user (is_enabled, updated_at);

CREATE INDEX idx_user_locked_lock_time
    ON tb_user (is_account_non_locked, lock_time);