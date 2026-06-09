package sneak_shop.controller.admin;

import org.springframework.web.bind.annotation.*;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.dto.request.BannerRequest;
import sneak_shop.dto.request.BannerReorderRequest;
import sneak_shop.entity.BannerEntity;
import sneak_shop.repository.BannerRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin/banners")
public class AdminBannerController {

    private static final int MAX_BANNERS = 9;

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
        if (bannerRepository.count() >= MAX_BANNERS) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chi duoc toi da 9 banner");
        }
        BannerEntity banner = new BannerEntity();
        applyCreate(banner, req);
        if (req.sortOrder() == null) {
            banner.setSortOrder(bannerRepository.findAllByOrderBySortOrderAscIdDesc().stream()
                    .map(BannerEntity::getSortOrder)
                    .max(Integer::compareTo)
                    .map(max -> max + 1)
                    .orElse(0));
        }
        return ApiResponse.ok("Da tao banner", bannerRepository.save(banner));
    }

    @PutMapping("/{id}")
    public ApiResponse<BannerEntity> update(@PathVariable Integer id, @RequestBody BannerRequest req) {
        BannerEntity banner = bannerRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Banner khong ton tai"));
        applyUpdate(banner, req);
        return ApiResponse.ok("Da cap nhat banner", bannerRepository.save(banner));
    }

    @PostMapping("/reorder")
    @Transactional
    public ApiResponse<List<BannerEntity>> reorder(@RequestBody BannerReorderRequest req) {
        if (req == null || req.ids() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Danh sach banner khong hop le");
        }

        List<BannerEntity> banners = bannerRepository.findAllById(req.ids());
        if (banners.size() != req.ids().size()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Danh sach banner khong hop le");
        }

        banners.sort(Comparator.comparingInt(b -> req.ids().indexOf(b.getId())));
        for (int i = 0; i < banners.size(); i++) {
            banners.get(i).setSortOrder(i);
        }

        return ApiResponse.ok("Da cap nhat thu tu banner", bannerRepository.saveAll(banners));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        if (!bannerRepository.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Banner khong ton tai");
        }
        bannerRepository.deleteById(id);
        normalizeOrder();
        return ApiResponse.ok("Da xoa banner");
    }

    private void applyCreate(BannerEntity banner, BannerRequest req) {
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

    private void applyUpdate(BannerEntity banner, BannerRequest req) {
        if (req == null || req.imageUrl() == null || req.imageUrl().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "ImageUrl khong hop le");
        }
        if (req.title() != null) {
            banner.setTitle(normalize(req.title()));
        }
        banner.setImageUrl(req.imageUrl().trim());
        if (req.linkUrl() != null) {
            banner.setLinkUrl(normalize(req.linkUrl()));
        }
        if (req.position() != null) {
            banner.setPosition(normalize(req.position()) != null ? normalize(req.position()) : "hero");
        }
        if (req.isActive() != null) {
            banner.setIsActive(req.isActive());
        }
        if (req.sortOrder() != null) {
            banner.setSortOrder(req.sortOrder());
        }
        if (req.startDate() != null) {
            banner.setStartDate(req.startDate());
        }
        if (req.endDate() != null) {
            banner.setEndDate(req.endDate());
        }
        if (banner.getCreatedAt() == null) {
            banner.setCreatedAt(LocalDateTime.now());
        }
        banner.setUpdatedAt(LocalDateTime.now());
    }

    private void normalizeOrder() {
        List<BannerEntity> banners = bannerRepository.findAllByOrderBySortOrderAscIdDesc();
        for (int i = 0; i < banners.size(); i++) {
            BannerEntity banner = banners.get(i);
            if (!Integer.valueOf(i).equals(banner.getSortOrder())) {
                banner.setSortOrder(i);
            }
        }
        bannerRepository.saveAll(banners);
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
