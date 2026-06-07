package sneak_shop.controller.admin;

import org.springframework.web.bind.annotation.*;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.dto.request.BannerRequest;
import sneak_shop.entity.BannerEntity;
import sneak_shop.repository.BannerRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
public class AdminBannerController {

    private final BannerRepository bannerRepository;

    public AdminBannerController(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    @GetMapping
    public ApiResponse<List<BannerEntity>> getAll() {
        return ApiResponse.ok(bannerRepository.findAllByOrderBySortOrderAscIdDesc());
    }

    @PostMapping
    public ApiResponse<BannerEntity> create(@RequestBody BannerRequest req) {
        BannerEntity banner = new BannerEntity();
        apply(banner, req);
        return ApiResponse.ok("Da tao banner", bannerRepository.save(banner));
    }

    @PutMapping("/{id}")
    public ApiResponse<BannerEntity> update(@PathVariable Integer id, @RequestBody BannerRequest req) {
        BannerEntity banner = bannerRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Banner khong ton tai"));
        apply(banner, req);
        return ApiResponse.ok("Da cap nhat banner", bannerRepository.save(banner));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        if (!bannerRepository.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Banner khong ton tai");
        }
        bannerRepository.deleteById(id);
        return ApiResponse.ok("Da xoa banner");
    }

    private void apply(BannerEntity banner, BannerRequest req) {
        if (req == null || req.imageUrl() == null || req.imageUrl().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "ImageUrl khong hop le");
        }
        banner.setTitle(normalize(req.title()));
        banner.setImageUrl(req.imageUrl().trim());
        banner.setLinkUrl(normalize(req.linkUrl()));
        banner.setPosition(normalize(req.position()) != null ? normalize(req.position()) : "hero");
        banner.setIsActive(req.isActive() == null || req.isActive());
        banner.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
        banner.setStartDate(req.startDate());
        banner.setEndDate(req.endDate());
        if (banner.getCreatedAt() == null) {
            banner.setCreatedAt(LocalDateTime.now());
        }
        banner.setUpdatedAt(LocalDateTime.now());
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
