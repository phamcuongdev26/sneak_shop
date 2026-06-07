package sneak_shop.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public ChatSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("chat_messages")) {
            return;
        }

        ensureColumn("chat_messages", "order_code", "VARCHAR(50) NULL");
        ensureColumn("chat_messages", "user_id", "INT NULL");
        ensureColumn("chat_messages", "sender_role", "VARCHAR(10) NULL");
        ensureColumn("chat_messages", "sender_name", "VARCHAR(200) NULL");

        // Old schema compatibility: let legacy columns stay harmless if the table was created before the refactor.
        if (columnExists("chat_messages", "conversation_id")) {
            jdbcTemplate.execute("ALTER TABLE chat_messages MODIFY COLUMN conversation_id INT NULL");
        }
        if (columnExists("chat_messages", "sender_type")) {
            jdbcTemplate.execute("ALTER TABLE chat_messages MODIFY COLUMN sender_type VARCHAR(20) NULL");
        }
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """,
                Integer.class,
                table
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                table,
                column
        );
        return count != null && count > 0;
    }

    private void ensureColumn(String table, String column, String definition) {
        if (!columnExists(table, column)) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}
