package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import sneak_shop.entity.FinancialLogEntity;

public interface FinancialLogRepository extends JpaRepository<FinancialLogEntity, Integer>,
        JpaSpecificationExecutor<FinancialLogEntity> {
}
