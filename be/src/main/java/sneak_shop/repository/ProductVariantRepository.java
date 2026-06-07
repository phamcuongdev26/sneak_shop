package sneak_shop.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sneak_shop.entity.ProductVariantEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Integer> {
    @EntityGraph(attributePaths = {"colors"})
    List<ProductVariantEntity> findByProductId(Integer productId);
    @EntityGraph(attributePaths = {"colors"})
    List<ProductVariantEntity> findByProductIdIn(Collection<Integer> productIds);
    Optional<ProductVariantEntity> findBySku(String sku);
    boolean existsBySku(String sku);
    void deleteByProductId(Integer productId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ProductVariantEntity v WHERE v.id = :variantId")
    void deleteVariantById(@Param("variantId") Integer variantId);
}
