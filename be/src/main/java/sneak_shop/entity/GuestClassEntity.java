package sneak_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import sneak_shop.enums.CustomerTier;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "guest_class")
public class GuestClassEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "rank_name", length = 20)
    @Builder.Default
    private CustomerTier rankName = CustomerTier.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "total_amount", nullable = false)
    @Builder.Default
    private Integer totalAmount = 0;

    @Column(name = "description", nullable = false, length = 25)
    @Builder.Default
    private String description = "new";

    @Column(name = "color", nullable = false, length = 100)
    @Builder.Default
    private String color = "gray";
}
