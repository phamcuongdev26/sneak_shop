package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sneak_shop.entity.ProductCategoryMappingEntity;

import java.util.Collection;
import java.util.List;

public interface ProductCategoryMappingRepository extends JpaRepository<ProductCategoryMappingEntity, Integer> {
    List<ProductCategoryMappingEntity> findByProductId(Integer productId);
    @EntityGraph(attributePaths = {"category"})
    List<ProductCategoryMappingEntity> findByProductIdIn(Collection<Integer> productIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ProductCategoryMappingEntity m WHERE m.product.id = :productId")
    void deleteByProductId(@Param("productId") Integer productId);

    boolean existsByCategoryId(Integer categoryId);
    boolean existsByProductIdAndCategoryId(Integer productId, Integer categoryId);
}
