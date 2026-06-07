-- product lookups
ALTER TABLE products ADD INDEX idx_products_status (status);
ALTER TABLE products ADD INDEX idx_products_slug (slug);

-- variant & color lookups
ALTER TABLE product_variants ADD INDEX idx_variants_product_id (product_id);
ALTER TABLE product_variant_colors ADD INDEX idx_colors_variant_id (variant_id);

-- reviews & order items (for avg rating, count, sold)
ALTER TABLE reviews ADD INDEX idx_reviews_product_id (product_id);
ALTER TABLE order_items ADD INDEX idx_order_items_product_id (product_id);

-- category mapping
ALTER TABLE product_category_mappings ADD INDEX idx_pcm_product_id (product_id);

-- product images
ALTER TABLE product_images ADD INDEX idx_product_images_product_id (product_id);
