ALTER TABLE view_change_history
    MODIFY COLUMN old_value TEXT NULL,
    MODIFY COLUMN new_value TEXT NULL;
