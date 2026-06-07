-- 1. Rename audit_logs → view_change_history, rename column table_name → function
RENAME TABLE audit_logs TO view_change_history;
ALTER TABLE view_change_history CHANGE COLUMN table_name `function` VARCHAR(50) NOT NULL;

-- 2. New financial/transaction audit log table
CREATE TABLE audit_log (
    id              INT          NOT NULL AUTO_INCREMENT,
    email           VARCHAR(100) NOT NULL,
    users_id        INT          NULL,
    addresses_id    INT          NULL,
    transactions_id INT          NULL,
    orders_id       INT          NULL,
    products_id     INT          NULL,
    products_shop_id INT         NULL,
    amount          BIGINT       NOT NULL,
    bank_name       VARCHAR(255) NOT NULL,
    note            TEXT         NULL,
    created_at      DATETIME     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_al_user        FOREIGN KEY (users_id)         REFERENCES users(id)          ON DELETE SET NULL,
    CONSTRAINT fk_al_address     FOREIGN KEY (addresses_id)     REFERENCES addresses(id)      ON DELETE SET NULL,
    CONSTRAINT fk_al_transaction FOREIGN KEY (transactions_id)  REFERENCES transactions(id)   ON DELETE SET NULL,
    CONSTRAINT fk_al_order       FOREIGN KEY (orders_id)        REFERENCES orders(id)         ON DELETE SET NULL,
    CONSTRAINT fk_al_product     FOREIGN KEY (products_id)      REFERENCES products(id)       ON DELETE SET NULL,
    CONSTRAINT fk_al_shop        FOREIGN KEY (products_shop_id) REFERENCES product_shops(id)  ON DELETE SET NULL
);
