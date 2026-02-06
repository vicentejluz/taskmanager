CREATE INDEX idx_task_status_updated_at ON tb_task(status, updated_at);

CREATE INDEX idx_task_status_due_date ON tb_task(status, due_date);

CREATE INDEX idx_task_due_date ON tb_task(due_date);