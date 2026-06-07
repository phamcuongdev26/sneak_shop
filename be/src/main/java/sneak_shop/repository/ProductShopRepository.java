package sneak_shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sneak_shop.entity.ProductShopEntity;

public interface ProductShopRepository extends JpaRepository<ProductShopEntity, Integer> {

    @Query("SELECT s FROM ProductShopEntity s WHERE :keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<ProductShopEntity> search(@Param("keyword") String keyword, Pageable pageable);
}
