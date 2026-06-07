-- ============================================================
-- DB SYNC — chạy trong DataGrip hoặc MySQL Workbench
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ─── 1. XOÁ BẢNG KHÔNG CÒN TRONG SCHEMA ─────────────────────
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS shop_users;
DROP TABLE IF EXISTS customer_tiers;
DROP TABLE IF EXISTS banners;
DROP TABLE IF EXISTS blog_posts;
DROP TABLE IF EXISTS chat_messages;
DROP TABLE IF EXISTS chat_conversations;
DROP TABLE IF EXISTS contact_messages;
DROP TABLE IF EXISTS coupon_usages;
DROP TABLE IF EXISTS coupons;
DROP TABLE IF EXISTS inventory_histories;
DROP TABLE IF EXISTS shipping_tracking;
DROP TABLE IF EXISTS site_settings;
DROP TABLE IF EXISTS voucher_usages;
DROP TABLE IF EXISTS vouchers;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS flyway_schema_history;

-- ─── 2. DROP BROKEN FK (từ customers đã bị xoá) ──────────────
-- Dùng procedure để tìm tên FK tự động (Hibernate sinh hash-name)
DROP PROCEDURE IF EXISTS sp_drop_fk;
DELIMITER $$
CREATE PROCEDURE sp_drop_fk(IN p_tbl VARCHAR(64), IN p_ref VARCHAR(64))
BEGIN
    DECLARE v_name VARCHAR(200) DEFAULT NULL;
    SELECT kcu.constraint_name INTO v_name
    FROM information_schema.key_column_usage kcu
    JOIN information_schema.table_constraints tc
        ON tc.constraint_name = kcu.constraint_name
        AND tc.table_schema    = kcu.table_schema
        AND tc.table_name      = kcu.table_name
    WHERE kcu.table_schema         = DATABASE()
      AND kcu.table_name           = p_tbl
      AND kcu.referenced_table_name = p_ref
      AND tc.constraint_type       = 'FOREIGN KEY'
    LIMIT 1;
    IF v_name IS NOT NULL THEN
        SET @s = CONCAT('ALTER TABLE `', p_tbl, '` DROP FOREIGN KEY `', v_name, '`');
        PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;
END $$
DELIMITER ;

CALL sp_drop_fk('addresses',     'customers');
CALL sp_drop_fk('cart_items',    'customers');
CALL sp_drop_fk('orders',        'customers');
CALL sp_drop_fk('orders',        'vouchers');
CALL sp_drop_fk('reviews',       'customers');
CALL sp_drop_fk('guest_class',   'customers');
CALL sp_drop_fk('notifications', 'customers');

DROP PROCEDURE IF EXISTS sp_drop_fk;

-- ─── 3. XOÁ orders.voucher_id (không có trong schema mới) ────
ALTER TABLE orders DROP COLUMN IF EXISTS voucher_id;

-- ─── 4. XOÁ bảng users cũ (UUID / RBAC-based) ───────────────
DROP TABLE IF EXISTS users;

-- ─── 5. TẠO bảng users mới ────────────────────────────────────
CREATE TABLE users (
    id         INT          NOT NULL AUTO_INCREMENT,
    shop_id    INT          NULL,
    address_id INT          NULL,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    full_name  VARCHAR(255) NOT NULL,
    phone      VARCHAR(20)  NULL,
    status     ENUM('active','inactive') NOT NULL DEFAULT 'active',
    role       ENUM('user','seller','admin') NOT NULL DEFAULT 'user',
    created_at DATETIME     NOT NULL DEFAULT NOW(),
    deleted_at DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    INDEX idx_users_email (email),
    CONSTRAINT fk_users_shop FOREIGN KEY (shop_id) REFERENCES product_shops(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── 6. THÊM FK cho các bảng con → users ──────────────────────
ALTER TABLE addresses
    ADD CONSTRAINT fk_addresses_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE cart_items
    ADD CONSTRAINT fk_cart_items_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_user
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_user
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE guest_class
    ADD CONSTRAINT fk_guest_class_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_user
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE;

-- ─── 7. THÊM FK address_id trên users → addresses ─────────────
ALTER TABLE users
    ADD CONSTRAINT fk_users_address
    FOREIGN KEY (address_id) REFERENCES addresses(id) ON DELETE SET NULL;

-- ─── 8. THÊM cột còn thiếu vào products ──────────────────────
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS rating_average DECIMAL(5,3) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS review_count   INT          NOT NULL DEFAULT 0;

-- ─── 9. THÊM cột còn thiếu vào product_shops ─────────────────
ALTER TABLE product_shops
    ADD COLUMN IF NOT EXISTS review_count INT NOT NULL DEFAULT 0;

-- ─── 10. XOÁ cột payment-gateway dư ra trong transactions ────
ALTER TABLE transactions
    DROP COLUMN IF EXISTS vnp_txn_ref,
    DROP COLUMN IF EXISTS vnp_transaction_no,
    DROP COLUMN IF EXISTS bank_code,
    DROP COLUMN IF EXISTS momo_order_id,
    DROP COLUMN IF EXISTS momo_trans_id,
    DROP COLUMN IF EXISTS raw_response;

-- Sửa enum payment_method & status cho đúng schema
ALTER TABLE transactions
    MODIFY COLUMN payment_method ENUM('cod','bank_transfer','e_wallet') NOT NULL,
    MODIFY COLUMN status         ENUM('pending','success','failed')     NOT NULL DEFAULT 'pending';

-- ─── 11. THÊM image_url & sửa type thành VARCHAR cho notifications ──
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(255) NULL,
    MODIFY COLUMN type VARCHAR(25) NOT NULL;

-- ─── 12. REBUILD audit_logs theo schema mới ──────────────────
DROP TABLE IF EXISTS audit_logs;
CREATE TABLE audit_logs (
    id         INT         NOT NULL AUTO_INCREMENT,
    table_name VARCHAR(50) NOT NULL,
    record_id  INT         NULL,
    old_value  VARCHAR(255) NULL,
    new_value  VARCHAR(255) NULL,
    action     ENUM('delete','update','insert') NOT NULL,
    compare    VARCHAR(255) NULL,
    created_at DATETIME    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ─── KIỂM TRA KẾT QUẢ ────────────────────────────────────────
SHOW TABLES;
