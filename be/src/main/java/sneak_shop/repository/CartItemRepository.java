package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sneak_shop.entity.CartItemEntity;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Integer> {
    List<CartItemEntity> findByUserId(Integer userId);

    @Query("""
        SELECT c FROM CartItemEntity c
        WHERE c.user.id = :userId
          AND c.product.id = :productId
          AND (:variantId IS NULL AND c.variant IS NULL OR c.variant.id = :variantId)
          AND (:colorId IS NULL AND c.color IS NULL OR c.color.id = :colorId)
        """)
    Optional<CartItemEntity> findExisting(
            @Param("userId") Integer userId,
            @Param("productId") Integer productId,
            @Param("variantId") Integer variantId,
            @Param("colorId") Integer colorId
    );

    void deleteByUserId(Integer userId);
    int countByUserId(Integer userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CartItemEntity c WHERE c.variant.id = :variantId")
    void deleteByVariantId(@Param("variantId") Integer variantId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CartItemEntity c WHERE c.color.id = :colorId")
    void deleteByColorId(@Param("colorId") Integer colorId);
}
