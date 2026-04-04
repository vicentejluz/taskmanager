-- Índice para consultas por task_id + status
CREATE INDEX idx_filemetadata_task_id_status
    ON tb_file_metadata(task_id, status);

-- Índice para consultas por id + status
CREATE INDEX idx_filemetadata_id_status
    ON tb_file_metadata(id, status);

-- Índice para consultas por stored_file_name
CREATE INDEX idx_filemetadata_stored_file_name
    ON tb_file_metadata(stored_file_name);