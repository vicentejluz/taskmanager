CREATE TABLE IF NOT EXISTS tb_file_metadata (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    path VARCHAR(255) NOT NULL,
    extension VARCHAR(10) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    size BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_file_metadata_task FOREIGN KEY(task_id) REFERENCES tb_tasks(id) ON DELETE CASCADE
);

CREATE INDEX idx_file_metadata_path
    ON tb_file_metadata(path);

CREATE INDEX idx_file_metadata_created_at
    ON tb_file_metadata(created_at);