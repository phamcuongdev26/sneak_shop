package sneak_shop.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.ReviewRequest;
import sneak_shop.dto.response.ReviewResponse;
import sneak_shop.security.UserContext;
import sneak_shop.service.impl.ReviewServiceImpl;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewServiceImpl reviewService;

    public ReviewController(ReviewServiceImpl reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/product/{productId}")
    public ApiResponse<PageResponse<ReviewResponse>> getByProduct(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(reviewService.getByProduct(productId, page, size));
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal UserContext ctx,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(reviewService.getMyReviews(ctx.id(), page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> create(@AuthenticationPrincipal UserContext ctx,
                                              @Valid @RequestBody ReviewRequest req) {
        return ApiResponse.ok("Danh gia thanh cong", reviewService.create(ctx.id(), req));
    }

    @PostMapping("/{reviewId}/reply")
    public ApiResponse<ReviewResponse> customerReply(
            @AuthenticationPrincipal UserContext ctx,
            @PathVariable Integer reviewId,
            @RequestBody @NotBlank String reply) {
        return ApiResponse.ok("Da tra loi", reviewService.customerReply(ctx.id(), reviewId, reply));
    }

    record UploadImageRequest(@NotNull Integer productId, @NotBlank String imageUrl) {}
    record UploadImageResponse(Integer productImageId, String imageUrl) {}

    @PostMapping("/upload-image")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UploadImageResponse> uploadReviewImage(
            @AuthenticationPrincipal UserContext ctx,
            @RequestBody UploadImageRequest req) {
        var result = reviewService.saveReviewImage(req.productId(), req.imageUrl());
        return ApiResponse.ok(new UploadImageResponse(result.getId(), result.getImageUrl()));
    }
}
