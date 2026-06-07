package sneak_shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sneak_shop.entity.GuestClassEntity;

import java.util.List;

public interface GuestClassRepository extends JpaRepository<GuestClassEntity, Integer> {
    Page<GuestClassEntity> findAllByOrderByIdDesc(Pageable pageable);
    List<GuestClassEntity> findByUserIdOrderByTotalAmountDesc(Integer userId);
    boolean existsByUserIdAndProductId(Integer userId, Integer productId);
}
