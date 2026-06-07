package sneak_shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sneak_shop.entity.ReviewEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Integer> {
    Page<ReviewEntity> findByProductIdOrderByCreatedAtDesc(Integer productId, Pageable pageable);
    Page<ReviewEntity> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    boolean existsByOrderItemId(Integer orderItemId);
    Optional<ReviewEntity> findByOrderItemId(Integer orderItemId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM ReviewEntity r WHERE r.product.id = :productId")
    Double avgRatingByProductId(@Param("productId") Integer productId);

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.product.id = :productId")
    Long countByProductId(@Param("productId") Integer productId);

    @Query("""
            SELECT r.product.id, COALESCE(AVG(r.rating), 0)
            FROM ReviewEntity r
            WHERE r.product.id IN :productIds
            GROUP BY r.product.id
            """)
    List<Object[]> avgRatingByProductIds(@Param("productIds") Collection<Integer> productIds);

    @Query("""
            SELECT r.product.id, COUNT(r)
            FROM ReviewEntity r
            WHERE r.product.id IN :productIds
            GROUP BY r.product.id
            """)
    List<Object[]> countByProductIds(@Param("productIds") Collection<Integer> productIds);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM ReviewEntity r")
    Double avgRatingAll();

    Page<ReviewEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
