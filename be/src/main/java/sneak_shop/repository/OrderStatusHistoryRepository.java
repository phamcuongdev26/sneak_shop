package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sneak_shop.entity.OrderStatusHistoryEntity;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistoryEntity, Integer> {
    List<OrderStatusHistoryEntity> findByOrderIdOrderByCreatedAtAsc(Integer orderId);
}
