package sneak_shop.service;

import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.ProductRequest;
import sneak_shop.dto.request.ProductVariantRequest;
import sneak_shop.dto.response.ProductResponse;
import sneak_shop.enums.ProductStatus;

import java.math.BigDecimal;

public interface ProductService {
    PageResponse<ProductResponse> search(String keyword, BigDecimal minPrice, BigDecimal maxPrice,
                                         Integer categoryId, ProductStatus status, int page, int size, String sort);
    ProductResponse getBySlug(String slug);
    ProductResponse getById(Integer id);
    ProductResponse create(ProductRequest req);
    ProductResponse update(Integer id, ProductRequest req);
    PageResponse<ProductResponse> adminSearch(String keyword, ProductStatus status, Boolean deleted, int page, int size);
    void restore(Integer id);
    void delete(Integer id);
    ProductResponse.VariantSummary addVariant(Integer productId, ProductVariantRequest req);
    void deleteVariant(Integer productId, Integer variantId);
}
