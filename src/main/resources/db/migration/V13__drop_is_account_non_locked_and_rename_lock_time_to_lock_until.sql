ALTER TABLE tb_user
    RENAME COLUMN lock_time TO lock_until;

ALTER TABLE tb_user
    DROP COLUMN is_account_non_locked;
