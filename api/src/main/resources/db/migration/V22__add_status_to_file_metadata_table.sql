CREATE TYPE file_metadata_status AS ENUM ('PENDING', 'PENDING_DELETE', 'ACTIVE');

ALTER TABLE IF EXISTS tb_file_metadata
    ADD COLUMN status file_metadata_status NOT NULL DEFAULT 'PENDING';