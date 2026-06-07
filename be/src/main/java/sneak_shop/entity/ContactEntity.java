package sneak_shop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "contacts")
public class ContactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(length = 300)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String message;

    // JSON array of image URLs, e.g. ["http://...", "http://..."]
    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(length = 20)
    @Builder.Default
    private String status = "pending"; // pending | replied

    @Column(name = "reply_text", columnDefinition = "TEXT")
    private String replyText;

    @Column(name = "replied_at")
    private Instant repliedAt;

    @Column(name = "user_id")
    private Integer userId;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "pending";
    }
}
