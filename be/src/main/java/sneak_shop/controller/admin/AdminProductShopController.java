package sneak_shop.controller.admin;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.ProductShopRequest;
import sneak_shop.dto.response.ProductShopResponse;
import sneak_shop.entity.ProductShopEntity;
import sneak_shop.repository.ProductShopRepository;

@RestController
@RequestMapping("/api/admin/product-shops")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductShopController {

    private final ProductShopRepository shopRepository;

    public AdminProductShopController(ProductShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductShopResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ProductShopEntity> result = shopRepository.search(
                keyword, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.ok(PageResponse.from(result.map(ProductShopResponse::from)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductShopResponse> getById(@PathVariable Integer id) {
        return ApiResponse.ok(ProductShopResponse.from(findShop(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductShopResponse> create(@Valid @RequestBody ProductShopRequest req) {
        ProductShopEntity shop = ProductShopEntity.builder()
                .name(req.name())
                .avatar(req.avatar())
                .build();
        shopRepository.save(shop);
        return ApiResponse.ok("Tao shop thanh cong", ProductShopResponse.from(shop));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductShopResponse> update(@PathVariable Integer id,
                                                   @Valid @RequestBody ProductShopRequest req) {
        ProductShopEntity shop = findShop(id);
        shop.setName(req.name());
        shop.setAvatar(req.avatar());
        shopRepository.save(shop);
        return ApiResponse.ok("Cap nhat shop thanh cong", ProductShopResponse.from(shop));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        if (!shopRepository.existsById(id))
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Shop khong ton tai");
        shopRepository.deleteById(id);
        return ApiResponse.ok("Da xoa shop");
    }

    private ProductShopEntity findShop(Integer id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Shop khong ton tai"));
    }
}
