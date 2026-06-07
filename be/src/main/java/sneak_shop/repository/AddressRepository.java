package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sneak_shop.entity.AddressEntity;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<AddressEntity, Integer> {
    List<AddressEntity> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Integer userId);
    Optional<AddressEntity> findByUserIdAndIsDefaultTrue(Integer userId);
    boolean existsByUserId(Integer userId);
}
