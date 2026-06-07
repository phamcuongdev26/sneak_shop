package sneak_shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sneak_shop.entity.ContactEntity;

public interface ContactRepository extends JpaRepository<ContactEntity, Integer> {

    Page<ContactEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ContactEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<ContactEntity> findByEmailOrderByCreatedAtDesc(String email, Pageable pageable);

    Page<ContactEntity> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    long countByStatus(String status);
}
