package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sneak_shop.entity.OrderItemEntity;

import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Integer> {
    List<OrderItemEntity> findByOrderId(Integer orderId);
    List<OrderItemEntity> findByProductId(Integer productId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE OrderItemEntity oi SET oi.variant = null WHERE oi.variant.id = :variantId")
    void clearVariantReference(@Param("variantId") Integer variantId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE OrderItemEntity oi SET oi.color = null WHERE oi.color.id = :colorId")
    void clearColorReference(@Param("colorId") Integer colorId);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItemEntity oi WHERE oi.product.id = :productId")
    Long sumSoldByProductId(@Param("productId") Integer productId);

    @Query("""
            SELECT oi.product.id, COALESCE(SUM(oi.quantity), 0)
            FROM OrderItemEntity oi
            WHERE oi.product.id IN :productIds
            GROUP BY oi.product.id
            """)
    List<Object[]> sumSoldByProductIds(@Param("productIds") Collection<Integer> productIds);
}
