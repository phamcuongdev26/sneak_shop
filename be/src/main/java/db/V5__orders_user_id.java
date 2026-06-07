package db;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class V5__orders_user_id extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();

        // orders: rename customer_id -> user_id
        if (columnExists(conn, "orders", "customer_id")) {
            exec(conn, "ALTER TABLE orders CHANGE COLUMN customer_id user_id INT NOT NULL, ALGORITHM=INPLACE, LOCK=NONE");
        }

        // orders: migrate momo -> e_wallet
        exec(conn, "UPDATE orders SET payment_method = 'e_wallet' WHERE payment_method = 'momo'");

        // orders: remove momo from payment_method enum
        exec(conn, "ALTER TABLE orders MODIFY COLUMN payment_method ENUM('cod','bank_transfer','e_wallet') NOT NULL");

        // addresses: rename customer_id -> user_id
        if (columnExists(conn, "addresses", "customer_id")) {
            exec(conn, "ALTER TABLE addresses CHANGE COLUMN customer_id user_id INT NOT NULL, ALGORITHM=INPLACE, LOCK=NONE");
        }

        // cart_items: drop FK constraints on customer_id before dropping the index it covers
        if (columnExists(conn, "cart_items", "customer_id")) {
            dropForeignKeysOnColumn(conn, "cart_items", "customer_id");
        }

        // cart_items: drop old unique index if exists
        if (indexExists(conn, "cart_items", "uk_cart_customer_product_variant_color")) {
            exec(conn, "ALTER TABLE cart_items DROP INDEX uk_cart_customer_product_variant_color");
        }

        // cart_items: rename customer_id -> user_id
        if (columnExists(conn, "cart_items", "customer_id")) {
            exec(conn, "ALTER TABLE cart_items CHANGE COLUMN customer_id user_id INT NOT NULL, ALGORITHM=INPLACE, LOCK=NONE");
        }

        // cart_items: add new unique index
        if (!indexExists(conn, "cart_items", "uk_cart_user_product_variant_color")) {
            exec(conn, "ALTER TABLE cart_items ADD UNIQUE KEY uk_cart_user_product_variant_color (user_id, product_id, variant_id, color_id)");
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

    private void dropForeignKeysOnColumn(Connection conn, String table, String column) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT kcu.CONSTRAINT_NAME " +
                "FROM information_schema.KEY_COLUMN_USAGE kcu " +
                "JOIN information_schema.TABLE_CONSTRAINTS tc " +
                "  ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA AND tc.TABLE_NAME = kcu.TABLE_NAME " +
                "WHERE tc.CONSTRAINT_TYPE = 'FOREIGN KEY' " +
                "  AND kcu.TABLE_SCHEMA = DATABASE() " +
                "  AND kcu.TABLE_NAME = '" + table + "' " +
                "  AND kcu.COLUMN_NAME = '" + column + "'")) {
            java.util.List<String> names = new java.util.ArrayList<>();
            while (rs.next()) names.add(rs.getString(1));
            for (String name : names) {
                try (Statement st2 = conn.createStatement()) {
                    st2.execute("ALTER TABLE " + table + " DROP FOREIGN KEY `" + name + "`");
                }
            }
        }
    }

    private boolean indexExists(Connection conn, String table, String indexName) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()" +
                " AND TABLE_NAME = '" + table + "' AND INDEX_NAME = '" + indexName + "'")) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }
}
