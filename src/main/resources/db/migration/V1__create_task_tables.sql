CREATE TYPE task_status AS ENUM ('IN_PROGRESS', 'PENDING', 'DONE', 'CANCELLED');

CREATE TABLE IF NOT EXISTS tb_task(
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(50) NOT NULL,
    description TEXT,
    due_date DATE NOT NULL,
    status task_status NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);


-- function para atualizar a data do updated_at
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW IS DISTINCT FROM OLD THEN
        NEW.updated_at := now();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger que antes do update na tabela tb_task ele chama a função set_updated_at()
CREATE TRIGGER trg_set_updated_at
BEFORE UPDATE ON tb_task
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();