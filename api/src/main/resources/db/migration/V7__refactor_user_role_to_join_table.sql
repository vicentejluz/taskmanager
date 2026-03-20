ALTER TABLE tb_user DROP COLUMN user_role;

CREATE TABLE tb_role(
    user_id BIGINT NOT NULL,
    role user_role NOT NULL,
    CONSTRAINT fk_role_user FOREIGN KEY (user_id) REFERENCES tb_user(id),
    CONSTRAINT pk_user_id_role PRIMARY KEY (user_id, role)
);

CREATE INDEX idx_role_user_id ON tb_role(user_id);