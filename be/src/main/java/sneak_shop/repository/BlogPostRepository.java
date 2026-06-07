package sneak_shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sneak_shop.entity.BlogPostEntity;

import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPostEntity, Integer> {
    Page<BlogPostEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    Optional<BlogPostEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Integer id);
}
