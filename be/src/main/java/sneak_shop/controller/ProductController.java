package sneak_shop.controller;

import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.response.ProductResponse;
import sneak_shop.enums.ProductStatus;
import sneak_shop.repository.ProductCategoryRepository;
import sneak_shop.service.impl.ProductServiceImpl;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductServiceImpl productService;
    private final ProductCategoryRepository categoryRepository;

    public ProductController(ProductServiceImpl productService, ProductCategoryRepository categoryRepository) {
        this.productService = productService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        Integer resolvedCategoryId = categoryId;
        if (resolvedCategoryId == null && categorySlug != null) {
            resolvedCategoryId = categoryRepository.findBySlug(categorySlug)
                    .map(c -> c.getId()).orElse(null);
        }
        return ApiResponse.ok(productService.search(keyword, minPrice, maxPrice, resolvedCategoryId,
                ProductStatus.active, page, size, sort));
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<ProductResponse> getBySlugPath(@PathVariable String slug) {
        return ApiResponse.ok(productService.getBySlug(slug));
    }

    @GetMapping("/{slug}")
    public ApiResponse<ProductResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.ok(productService.getBySlug(slug));
    }

    @GetMapping("/id/{id}")
    public ApiResponse<ProductResponse> getById(@PathVariable Integer id) {
        return ApiResponse.ok(productService.getById(id));
    }
}
