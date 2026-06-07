package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sneak_shop.entity.ReviewImageEntity;

import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImageEntity, Integer> {
    List<ReviewImageEntity> findByReviewId(Integer reviewId);
}
