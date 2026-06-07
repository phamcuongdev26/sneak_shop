-- V3: align schema with spec

-- orders: add shop_id and address_id
ALTER TABLE orders
  ADD COLUMN shop_id INT NULL,
  ADD COLUMN address_id INT NULL,
  ADD CONSTRAINT fk_orders_shop    FOREIGN KEY (shop_id)    REFERENCES product_shops(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_orders_address FOREIGN KEY (address_id) REFERENCES addresses(id)     ON DELETE SET NULL;

-- orders: migrate vnpay -> bank_transfer, keep momo as-is
UPDATE orders SET payment_method = 'bank_transfer' WHERE payment_method = 'vnpay';
ALTER TABLE orders
  MODIFY COLUMN payment_method ENUM('cod','bank_transfer','e_wallet','momo') NOT NULL;

-- order_items: drop duplicate/redundant columns
ALTER TABLE order_items
  DROP COLUMN product_image_url,
  DROP COLUMN total_price;

-- addresses: rename phone -> recipient_phone, address_line -> address, add updated_at
ALTER TABLE addresses
  CHANGE COLUMN phone         recipient_phone VARCHAR(20)  NOT NULL,
  CHANGE COLUMN address_line  address         VARCHAR(500) NOT NULL,
  ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- cart_items: add product_image snapshot column
ALTER TABLE cart_items
  ADD COLUMN product_image VARCHAR(500) NULL;
