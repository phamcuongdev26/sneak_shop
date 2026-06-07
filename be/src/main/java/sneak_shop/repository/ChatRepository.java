package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sneak_shop.entity.ChatMessageEntity;
import java.util.List;

public interface ChatRepository extends JpaRepository<ChatMessageEntity, Integer> {
    List<ChatMessageEntity> findByOrderCodeOrderByCreatedAtAsc(String orderCode);

    long countByIsReadFalseAndSenderRole(String senderRole);

    @Modifying
    @Query("UPDATE ChatMessageEntity c SET c.isRead = true WHERE c.orderCode = :orderCode AND c.senderRole = 'USER'")
    void markUserMessagesAsRead(@Param("orderCode") String orderCode);

    @Modifying
    @Query("UPDATE ChatMessageEntity c SET c.isRead = true WHERE c.orderCode = :orderCode AND c.senderRole = 'ADMIN'")
    void markAdminMessagesAsRead(@Param("orderCode") String orderCode);

    // Native query: list conversations with last message info and unread count
    @Query(value = """
        SELECT c.order_code,
               COALESCE(
                   (SELECT m.sender_name
                    FROM chat_messages m
                    WHERE m.order_code = c.order_code
                      AND m.sender_role = 'USER'
                    ORDER BY m.created_at DESC, m.id DESC
                    LIMIT 1),
                   (SELECT m.sender_name
                    FROM chat_messages m
                    WHERE m.order_code = c.order_code
                    ORDER BY m.created_at DESC, m.id DESC
                    LIMIT 1),
                   c.order_code
               ) as display_name,
               MAX(c.created_at) as last_time,
               SUM(CASE WHEN c.is_read = 0 AND c.sender_role = 'USER' THEN 1 ELSE 0 END) as unread_count,
               (SELECT content FROM chat_messages m WHERE m.order_code = c.order_code ORDER BY m.created_at DESC LIMIT 1) as last_content,
               (SELECT sender_role FROM chat_messages m WHERE m.order_code = c.order_code ORDER BY m.created_at DESC LIMIT 1) as last_sender_role
        FROM chat_messages c
        GROUP BY c.order_code
        ORDER BY last_time DESC
        """, nativeQuery = true)
    List<Object[]> findConversations();
}
