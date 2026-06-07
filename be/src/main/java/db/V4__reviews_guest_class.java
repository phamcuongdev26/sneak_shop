package db;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class V4__reviews_guest_class extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();

        // reviews: rename customer_id -> user_id
        if (columnExists(conn, "reviews", "customer_id")) {
            exec(conn, "ALTER TABLE reviews CHANGE COLUMN customer_id user_id INT NOT NULL, ALGORITHM=INPLACE, LOCK=NONE");
        }

        // reviews: add shop_id
        if (!columnExists(conn, "reviews", "shop_id")) {
            exec(conn, "ALTER TABLE reviews ADD COLUMN shop_id INT NULL");
        }
        if (!constraintExists(conn, "reviews", "fk_reviews_shop")) {
            exec(conn, "ALTER TABLE reviews ADD CONSTRAINT fk_reviews_shop FOREIGN KEY (shop_id) REFERENCES product_shops(id) ON DELETE SET NULL");
        }

        // reviews: rename replied_at -> shop_reply_at
        if (columnExists(conn, "reviews", "replied_at")) {
            exec(conn, "ALTER TABLE reviews CHANGE COLUMN replied_at shop_reply_at DATETIME NULL");
        }
        if (!columnExists(conn, "reviews", "reply")) {
            exec(conn, "ALTER TABLE reviews ADD COLUMN reply VARCHAR(300) NULL, ADD COLUMN reply_at DATETIME NULL");
        }

        // rename review_images -> reviews_image
        if (tableExists(conn, "review_images") && !tableExists(conn, "reviews_image")) {
            exec(conn, "RENAME TABLE review_images TO reviews_image");
        }

        // reviews_image: rename review_id -> reviews_id
        if (columnExists(conn, "reviews_image", "review_id")) {
            exec(conn, "ALTER TABLE reviews_image CHANGE COLUMN review_id reviews_id INT NOT NULL, ALGORITHM=INPLACE, LOCK=NONE");
        }

        // reviews_image: add product_image_id, drop old columns
        if (!columnExists(conn, "reviews_image", "product_image_id")) {
            exec(conn, "ALTER TABLE reviews_image ADD COLUMN product_image_id INT NULL");
        }
        if (columnExists(conn, "reviews_image", "image_url")) {
            exec(conn, "ALTER TABLE reviews_image DROP COLUMN image_url");
        }
        if (columnExists(conn, "reviews_image", "sort_order")) {
            exec(conn, "ALTER TABLE reviews_image DROP COLUMN sort_order");
        }
        if (!constraintExists(conn, "reviews_image", "fk_ri_product_image")) {
            exec(conn, "ALTER TABLE reviews_image ADD CONSTRAINT fk_ri_product_image FOREIGN KEY (product_image_id) REFERENCES product_images(id) ON DELETE CASCADE");
        }

        // guest_class
        if (!tableExists(conn, "guest_class")) {
            exec(conn,
                "CREATE TABLE guest_class (" +
                "  id           INT          PRIMARY KEY AUTO_INCREMENT," +
                "  user_id      INT          NOT NULL," +
                "  rank_name    ENUM('NEW','BRONZE','SILVER','GOLD','VIP') NOT NULL DEFAULT 'NEW'," +
                "  product_id   INT          NOT NULL," +
                "  total_amount INT          NOT NULL DEFAULT 0," +
                "  description  VARCHAR(25)  NOT NULL DEFAULT 'new'," +
                "  color        VARCHAR(100) NOT NULL DEFAULT 'gray'," +
                "  CONSTRAINT fk_gc_user    FOREIGN KEY (user_id)    REFERENCES customers(id) ON DELETE CASCADE," +
                "  CONSTRAINT fk_gc_product FOREIGN KEY (product_id) REFERENCES products(id)  ON DELETE CASCADE" +
                ")"
            );
        }
    }

    private void exec(Connection conn, String sql) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    private boolean columnExists(Connection conn, String table, String column) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()" +
                " AND TABLE_NAME = '" + table + "' AND COLUMN_NAME = '" + column + "'")) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private boolean tableExists(Connection conn, String table) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()" +
                " AND TABLE_NAME = '" + table + "'")) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private boolean constraintExists(Connection conn, String table, String constraint) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE()" +
                " AND TABLE_NAME = '" + table + "' AND CONSTRAINT_NAME = '" + constraint + "'")) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }
}
