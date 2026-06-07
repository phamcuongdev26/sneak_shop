package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sneak_shop.entity.BannerEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface BannerRepository extends JpaRepository<BannerEntity, Integer> {

    @Query("""
            SELECT b
            FROM BannerEntity b
            WHERE b.isActive = true
              AND (b.startDate IS NULL OR b.startDate <= :now)
              AND (b.endDate IS NULL OR b.endDate >= :now)
            ORDER BY b.sortOrder ASC, b.id DESC
            """)
    List<BannerEntity> findActiveForDisplay(@Param("now") LocalDateTime now);

    List<BannerEntity> findAllByOrderBySortOrderAscIdDesc();
}
