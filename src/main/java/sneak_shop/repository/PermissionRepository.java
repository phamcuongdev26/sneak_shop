package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sneak_shop.entity.PermissionEntity;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<PermissionEntity, String> {

	Optional<PermissionEntity> findByName(String name);
}
