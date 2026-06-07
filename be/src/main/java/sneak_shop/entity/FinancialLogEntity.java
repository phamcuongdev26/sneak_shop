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
@Table(name = "audit_log")
public class FinancialLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "users_id")
    private Integer usersId;

    @Column(name = "addresses_id")
    private Integer addressesId;

    @Column(name = "transactions_id")
    private Integer transactionsId;

    @Column(name = "orders_id")
    private Integer ordersId;

    @Column(name = "products_id")
    private Integer productsId;

    @Column(name = "products_shop_id")
    private Integer productsShopId;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
