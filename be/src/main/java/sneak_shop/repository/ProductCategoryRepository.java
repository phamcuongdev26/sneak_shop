package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sneak_shop.entity.ProductCategoryEntity;
import sneak_shop.enums.CategoryStatus;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategoryEntity, Integer> {
    Optional<ProductCategoryEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<ProductCategoryEntity> findByParentIsNullAndStatus(CategoryStatus status);
    List<ProductCategoryEntity> findByParentIdAndStatus(Integer parentId, CategoryStatus status);
    List<ProductCategoryEntity> findByStatusOrderBySortOrderAsc(CategoryStatus status);
}
