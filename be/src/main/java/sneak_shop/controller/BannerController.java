package sneak_shop.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.entity.BannerEntity;
import sneak_shop.repository.BannerRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    private final BannerRepository bannerRepository;

    public BannerController(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    @GetMapping
    public ApiResponse<List<BannerEntity>> getActive() {
        return ApiResponse.ok(bannerRepository.findActiveForDisplay(LocalDateTime.now()));
    }
}
