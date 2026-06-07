package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sneak_shop.entity.ProductImageEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Integer> {
    List<ProductImageEntity> findByProductIdOrderBySortOrderAsc(Integer productId);
    List<ProductImageEntity> findByProductIdAndTypeNotOrderBySortOrderAsc(Integer productId, String type);
    List<ProductImageEntity> findByProductIdInAndTypeNotOrderByProductIdAscSortOrderAsc(Collection<Integer> productIds, String type);
    Optional<ProductImageEntity> findFirstByProductIdAndType(Integer productId, String type);
    void deleteByProductId(Integer productId);
}
