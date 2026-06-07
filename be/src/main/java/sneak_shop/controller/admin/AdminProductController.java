package sneak_shop.controller.admin;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.ProductRequest;
import sneak_shop.dto.request.ProductVariantRequest;
import sneak_shop.dto.response.ProductResponse;
import sneak_shop.enums.ProductStatus;
import sneak_shop.service.impl.ProductServiceImpl;


@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_VIEW')")
public class AdminProductController {

    private final ProductServiceImpl productService;

    public AdminProductController(ProductServiceImpl productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(productService.adminSearch(keyword, status, deleted, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getById(@PathVariable Integer id) {
        return ApiResponse.ok(productService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_CREATE')")
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
        return ApiResponse.ok("Tao san pham thanh cong", productService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    public ApiResponse<ProductResponse> update(@PathVariable Integer id, @Valid @RequestBody ProductRequest req) {
        return ApiResponse.ok(productService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_DELETE')")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        productService.delete(id);
        return ApiResponse.ok("Xoa san pham thanh cong");
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    public ApiResponse<Void> restore(@PathVariable Integer id) {
        productService.restore(id);
        return ApiResponse.ok("Khoi phuc san pham thanh cong");
    }

    @PostMapping("/{id}/variants")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    public ApiResponse<ProductResponse.VariantSummary> addVariant(@PathVariable Integer id,
                                                                    @Valid @RequestBody ProductVariantRequest req) {
        return ApiResponse.ok(productService.addVariant(id, req));
    }

    @PutMapping("/{id}/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    public ApiResponse<ProductResponse.VariantSummary> updateVariant(@PathVariable Integer id,
                                                                     @PathVariable Integer variantId,
                                                                     @Valid @RequestBody ProductVariantRequest req) {
        return ApiResponse.ok(productService.updateVariant(id, variantId, req));
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    public ApiResponse<Void> deleteVariant(@PathVariable Integer id, @PathVariable Integer variantId) {
        productService.deleteVariant(id, variantId);
        return ApiResponse.ok("Xoa variant thanh cong");
    }
}
