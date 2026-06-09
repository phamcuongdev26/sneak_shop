package sneak_shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sneak_shop.entity.UserEntity;
import sneak_shop.enums.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {

    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByPhone(String phone);
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByZaloId(String zaloId);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByUsername(String username);

    @Query("""
            SELECT COUNT(u) > 0 FROM UserEntity u
            WHERE u.email = :email AND u.id <> :id
            """)
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") Integer id);

    @Query("""
            SELECT COUNT(u) > 0 FROM UserEntity u
            WHERE u.username = :username AND u.id <> :id
            """)
    boolean existsByUsernameAndIdNot(@Param("username") String username, @Param("id") Integer id);

    @Query("SELECT u FROM UserEntity u WHERE u.role = :role AND u.deletedAt IS NULL")
    Page<UserEntity> findByRoleActive(@Param("role") UserRole role, Pageable pageable);

    List<UserEntity> findAllByRoleAndDeletedAtIsNull(UserRole role);

    @Query("""
            SELECT u FROM UserEntity u
            WHERE (:keyword IS NULL OR LOWER(u.email) LIKE %:keyword% OR LOWER(u.username) LIKE %:keyword% OR LOWER(u.fullName) LIKE %:keyword%)
              AND (:role IS NULL OR u.role = :role)
            ORDER BY u.createdAt DESC
            """)
    Page<UserEntity> search(@Param("keyword") String keyword,
                            @Param("role") UserRole role,
                            Pageable pageable);

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.createdAt >= :from AND u.createdAt < :to")
    Long countNewBetween(@Param("from") Instant from, @Param("to") Instant to);
}
