package sneak_shop.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.ReviewRequest;
import sneak_shop.dto.request.ShopReplyRequest;
import sneak_shop.dto.response.ReviewResponse;
import sneak_shop.entity.*;
import sneak_shop.enums.OrderStatus;
import sneak_shop.repository.*;
import sneak_shop.service.ReviewService;
import sneak_shop.service.NotificationService;

import java.time.Instant;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final NotificationService notificationService;

    public ReviewServiceImpl(ReviewRepository reviewRepository, UserRepository userRepository,
                             OrderItemRepository orderItemRepository, ProductRepository productRepository,
                             ProductImageRepository productImageRepository,
                             ReviewImageRepository reviewImageRepository,
                             NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.reviewImageRepository = reviewImageRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAll(int page, int size) {
        return PageResponse.from(reviewRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size)).map(ReviewResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getByProduct(Integer productId, int page, int size) {
        return PageResponse.from(reviewRepository.findByProductIdOrderByCreatedAtDesc(
                productId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(ReviewResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getMyReviews(Integer userId, int page, int size) {
        return PageResponse.from(reviewRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size)).map(ReviewResponse::from));
    }

    @Transactional
    public ReviewResponse create(Integer userId, ReviewRequest req) {
        if (reviewRepository.existsByOrderItemId(req.orderItemId())) {
            throw new AppException(ErrorCode.CONFLICT, "Ban da danh gia san pham nay roi");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User khong ton tai"));
        OrderItemEntity orderItem = orderItemRepository.findById(req.orderItemId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Order item khong ton tai"));
        if (!orderItem.getOrder().getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Khong duoc danh gia don hang cua nguoi khac");
        }
        if (orderItem.getOrder().getStatus() != OrderStatus.completed) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chi co the danh gia don hang da hoan thanh");
        }
        ProductEntity product = orderItem.getProduct();

        ReviewEntity review = ReviewEntity.builder()
                .user(user).orderItem(orderItem).product(product)
                .rating(req.rating()).comment(req.comment()).build();
        review = reviewRepository.save(review);

        if (req.productImageIds() != null && !req.productImageIds().isEmpty()) {
            ReviewEntity finalReview = review;
            var images = req.productImageIds().stream().limit(5)
                    .flatMap(imgId -> productImageRepository.findById(imgId).stream())
                    .map(pi -> ReviewImageEntity.builder()
                            .review(finalReview)
                            .productImage(pi)
                            .build())
                    .toList();
            if (!images.isEmpty()) reviewImageRepository.saveAll(images);
        }

        review.getImages().clear();
        review.getImages().addAll(reviewImageRepository.findByReviewId(review.getId()));
        notificationService.notifyAdmins(
                "Co danh gia moi",
                "San pham " + product.getName() + " vua co mot danh gia moi.",
                "review_new",
                null
        );
        return ReviewResponse.from(review);
    }

    @Transactional
    public ReviewResponse shopReply(Integer reviewId, ShopReplyRequest req) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Review khong ton tai"));
        review.setShopReply(req.reply());
        review.setShopReplyAt(Instant.now());
        review = reviewRepository.save(review);
        notificationService.notifyUser(
                review.getUser().getId(),
                "Shop đã phản hồi",
                "Shop vừa phản hồi đánh giá của bạn cho sản phẩm " + review.getProduct().getName() + ".",
                "review_reply",
                null
        );
        return ReviewResponse.from(review);
    }

    @Transactional
    public ProductImageEntity saveReviewImage(Integer productId, String imageUrl) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "San pham khong ton tai"));
        return productImageRepository.save(ProductImageEntity.builder()
                .product(product)
                .imageUrl(imageUrl.trim())
                .type("review")
                .sortOrder(0)
                .build());
    }

    @Transactional
    public ReviewResponse customerReply(Integer userId, Integer reviewId, String reply) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Review khong ton tai"));
        if (!review.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Khong co quyen sua review nay");
        }
        if (review.getShopReply() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Shop chua phan hoi, chua the tra loi");
        }
        review.setReply(reply);
        review.setReplyAt(Instant.now());
        return ReviewResponse.from(reviewRepository.save(review));
    }
}
