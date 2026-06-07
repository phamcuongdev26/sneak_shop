package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sneak_shop.entity.TransactionEntity;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Integer> {
    List<TransactionEntity> findByOrderId(Integer orderId);
    Optional<TransactionEntity> findByTransactionCode(String transactionCode);
}
