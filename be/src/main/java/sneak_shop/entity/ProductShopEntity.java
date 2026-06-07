package sneak_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@BatchSize(size = 20)
@Table(name = "product_shops", indexes = @Index(name = "idx_product_shops_name", columnList = "name"))
public class ProductShopEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 250)
    private String name;

    @Column(length = 500)
    private String avatar;

    @Column(name = "product_quantity", nullable = false)
    @Builder.Default
    private Integer productQuantity = 0;

    @Column(name = "rating_average", precision = 5, scale = 3)
    @Builder.Default
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PrePersist
    void onCreate() {
        if (productQuantity == null) productQuantity = 0;
        if (ratingAverage == null) ratingAverage = BigDecimal.ZERO;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
