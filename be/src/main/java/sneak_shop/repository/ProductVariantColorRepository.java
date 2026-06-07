package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sneak_shop.entity.ProductVariantColorEntity;

import java.util.Collection;
import java.util.List;

public interface ProductVariantColorRepository extends JpaRepository<ProductVariantColorEntity, Integer> {
    List<ProductVariantColorEntity> findByVariantId(Integer variantId);
    @Query("SELECT c FROM ProductVariantColorEntity c JOIN FETCH c.variant WHERE c.variant.product.id = :productId")
    List<ProductVariantColorEntity> findByVariantProductId(@Param("productId") Integer productId);

    @Query("""
            SELECT DISTINCT c.variant.product.id, c.color
            FROM ProductVariantColorEntity c
            WHERE c.variant.product.id IN :productIds
            ORDER BY c.variant.product.id, c.color
            """)
    List<Object[]> findColorNamesByProductIds(@Param("productIds") Collection<Integer> productIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ProductVariantColorEntity c WHERE c.variant.id = :variantId")
    void deleteByVariantId(@Param("variantId") Integer variantId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ProductVariantColorEntity c WHERE c.variant.product.id = :productId")
    void deleteByVariantProductId(@Param("productId") Integer productId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductVariantColorEntity c SET c.stockQuantity = c.stockQuantity - :qty WHERE c.id = :id AND c.stockQuantity >= :qty")
    int deductStock(@Param("id") Integer id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductVariantColorEntity c SET c.stockQuantity = c.stockQuantity + :qty WHERE c.id = :id")
    void addStock(@Param("id") Integer id, @Param("qty") int qty);
}
