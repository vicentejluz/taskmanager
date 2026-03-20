ALTER TABLE tb_task
    ADD COLUMN user_id BIGINT;

ALTER TABLE tb_task
ADD CONSTRAINT fk_task_user
    FOREIGN KEY (user_id)
    REFERENCES tb_user(id);