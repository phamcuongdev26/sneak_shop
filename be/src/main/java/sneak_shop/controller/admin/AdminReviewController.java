package sneak_shop.controller.admin;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.ShopReplyRequest;
import sneak_shop.dto.response.ReviewResponse;
import sneak_shop.service.impl.ReviewServiceImpl;

@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewServiceImpl reviewService;

    public AdminReviewController(ReviewServiceImpl reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ReviewResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(reviewService.getAll(page, size));
    }

    @GetMapping("/product/{productId}")
    public ApiResponse<PageResponse<ReviewResponse>> getByProduct(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(reviewService.getByProduct(productId, page, size));
    }

    @PostMapping("/{reviewId}/reply")
    public ApiResponse<ReviewResponse> reply(@PathVariable Integer reviewId,
                                             @Valid @RequestBody ShopReplyRequest req) {
        return ApiResponse.ok(reviewService.shopReply(reviewId, req));
    }
}
