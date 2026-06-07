package sneak_shop.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.entity.BlogPostEntity;
import sneak_shop.repository.BlogPostRepository;

@RestController
@RequestMapping("/api/blog")
public class BlogController {

    private final BlogPostRepository blogPostRepository;

    public BlogController(BlogPostRepository blogPostRepository) {
        this.blogPostRepository = blogPostRepository;
    }

    @GetMapping
    public ApiResponse<PageResponse<BlogPostEntity>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok(PageResponse.from(blogPostRepository.findByStatusOrderByCreatedAtDesc("published", pageable)));
    }

    @GetMapping("/{slug}")
    public ApiResponse<BlogPostEntity> getBySlug(@PathVariable String slug) {
        BlogPostEntity post = blogPostRepository.findBySlug(slug)
                .filter(p -> "published".equalsIgnoreCase(p.getStatus()))
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Bai viet khong ton tai"));
        return ApiResponse.ok(post);
    }
}
