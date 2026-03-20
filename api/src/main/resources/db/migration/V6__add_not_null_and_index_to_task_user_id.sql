ALTER TABLE tb_task ALTER COLUMN user_id SET NOT NULL;

CREATE INDEX idx_task_user_id ON tb_task(user_id);