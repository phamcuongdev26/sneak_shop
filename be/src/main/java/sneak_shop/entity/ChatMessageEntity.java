package sneak_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_order_code", columnList = "order_code")
})
public class ChatMessageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_code", nullable = false, length = 50)
    private String orderCode;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "sender_role", length = 10, nullable = false)
    private String senderRole; // "USER" or "ADMIN"

    @Column(name = "sender_name", length = 200)
    private String senderName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
}
