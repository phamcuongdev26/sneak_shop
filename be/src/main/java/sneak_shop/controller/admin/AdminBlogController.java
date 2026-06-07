package sneak_shop.controller.admin;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.entity.BlogPostEntity;
import sneak_shop.repository.BlogPostRepository;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/blog")
public class AdminBlogController {

    private final BlogPostRepository blogPostRepository;

    public AdminBlogController(BlogPostRepository blogPostRepository) {
        this.blogPostRepository = blogPostRepository;
    }

    @GetMapping
    public ApiResponse<PageResponse<BlogPostEntity>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok(PageResponse.from(blogPostRepository.findAll(pageable)));
    }

    @PostMapping
    public ApiResponse<BlogPostEntity> create(@RequestBody BlogPostEntity req) {
        BlogPostEntity post = new BlogPostEntity();
        apply(post, req);
        return ApiResponse.ok("Da tao bai viet", blogPostRepository.save(post));
    }

    @PutMapping("/{id}")
    public ApiResponse<BlogPostEntity> update(@PathVariable Integer id, @RequestBody BlogPostEntity req) {
        BlogPostEntity post = blogPostRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Bai viet khong ton tai"));
        apply(post, req);
        return ApiResponse.ok("Da cap nhat bai viet", blogPostRepository.save(post));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        if (!blogPostRepository.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Bai viet khong ton tai");
        }
        blogPostRepository.deleteById(id);
        return ApiResponse.ok("Da xoa bai viet");
    }

    private void apply(BlogPostEntity post, BlogPostEntity req) {
        if (req == null || req.getTitle() == null || req.getTitle().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Tieu de khong hop le");
        }
        if (req.getSlug() == null || req.getSlug().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Slug khong hop le");
        }
        if (post.getId() == null) {
            if (blogPostRepository.existsBySlug(req.getSlug().trim())) {
                throw new AppException(ErrorCode.CONFLICT, "Slug da ton tai");
            }
        } else if (!req.getSlug().equals(post.getSlug()) && blogPostRepository.existsBySlugAndIdNot(req.getSlug().trim(), post.getId())) {
            throw new AppException(ErrorCode.CONFLICT, "Slug da ton tai");
        }
        post.setTitle(req.getTitle().trim());
        post.setSlug(req.getSlug().trim());
        post.setSummary(normalize(req.getSummary()));
        post.setContent(normalize(req.getContent()));
        post.setCoverImageUrl(normalize(req.getCoverImageUrl()));
        post.setStatus(normalizeStatus(req.getStatus()));
        if ("published".equalsIgnoreCase(post.getStatus()) && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
        if ("draft".equalsIgnoreCase(post.getStatus())) {
            post.setPublishedAt(null);
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeStatus(String status) {
        String normalized = normalize(status);
        if (normalized == null) return "draft";
        if (!normalized.equalsIgnoreCase("draft") && !normalized.equalsIgnoreCase("published")) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Trang thai khong hop le");
        }
        return normalized.toLowerCase();
    }
}
