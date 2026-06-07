package sneak_shop.service.impl;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.dto.request.CartRequest;
import sneak_shop.dto.response.CartItemResponse;
import sneak_shop.entity.*;
import sneak_shop.enums.ProductStatus;
import sneak_shop.repository.*;
import sneak_shop.service.CartService;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantColorRepository colorRepository;

    public CartServiceImpl(CartItemRepository cartItemRepository, UserRepository userRepository,
                           ProductRepository productRepository, ProductVariantRepository variantRepository,
                           ProductVariantColorRepository colorRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.colorRepository = colorRepository;
    }

    @Transactional
    public List<CartItemResponse> getCart(Integer userId) {
        return cartItemRepository.findByUserId(userId)
                .stream().map(CartItemResponse::from).toList();
    }

    @Transactional
    public CartItemResponse addOrUpdate(Integer userId, CartRequest req) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User khong ton tai"));
        ProductEntity product = productRepository.findById(req.productId())
                .filter(p -> !p.isDeleted() && p.getStatus() == ProductStatus.active)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "San pham khong ton tai"));

        ProductVariantEntity variant = null;
        if (req.variantId() != null) {
            variant = variantRepository.findById(req.variantId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Variant khong ton tai"));
            if (!variant.getProduct().getId().equals(product.getId()))
                throw new AppException(ErrorCode.INVALID_REQUEST, "Variant khong thuoc san pham nay");
        }

        ProductVariantColorEntity color = null;
        if (req.colorId() != null) {
            if (variant == null)
                throw new AppException(ErrorCode.INVALID_REQUEST, "Phai chon size truoc khi chon mau");
            color = colorRepository.findById(req.colorId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Mau sac khong ton tai"));
            if (!color.getVariant().getId().equals(variant.getId()))
                throw new AppException(ErrorCode.INVALID_REQUEST, "Mau sac khong thuoc size nay");
        }

        String productImage = color != null && color.getImageUrl() != null
                ? color.getImageUrl() : product.getCoverImageUrl();

        CartItemEntity item = cartItemRepository
                .findExisting(userId, req.productId(), req.variantId(), req.colorId())
                .orElse(CartItemEntity.builder()
                        .user(user).product(product)
                        .variant(variant).color(color).quantity(0)
                        .productImage(productImage)
                        .build());

        int newQty = item.getQuantity() + req.quantity();

        if (color != null) {
            if (color.getStockQuantity() <= 0)
                throw new AppException(ErrorCode.INVALID_REQUEST, "Mau nay da het hang");
            if (newQty > color.getStockQuantity())
                throw new AppException(ErrorCode.INVALID_REQUEST, "Chi con " + color.getStockQuantity() + " san pham");
        } else if (variant == null && product.getStockQuantity() != null) {
            int stock = product.getStockQuantity();
            if (stock <= 0)
                throw new AppException(ErrorCode.INVALID_REQUEST, "San pham nay da het hang");
            if (newQty > stock)
                throw new AppException(ErrorCode.INVALID_REQUEST, "Chi con " + stock + " san pham");
        }

        item.setQuantity(newQty);
        return CartItemResponse.from(cartItemRepository.save(item));
    }

    @Transactional
    public CartItemResponse updateQuantity(Integer userId, Integer itemId, Integer quantity) {
        CartItemEntity item = cartItemRepository.findById(itemId)
                .filter(i -> i.getUser().getId().equals(userId))
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Cart item khong ton tai"));
        if (quantity <= 0) { cartItemRepository.delete(item); return null; }

        if (item.getColor() != null) {
            int stock = item.getColor().getStockQuantity();
            if (quantity > stock)
                throw new AppException(ErrorCode.INVALID_REQUEST, "Chi con " + stock + " san pham");
        }
        item.setQuantity(quantity);
        return CartItemResponse.from(cartItemRepository.save(item));
    }

    @Transactional
    public void removeItem(Integer userId, Integer itemId) {
        cartItemRepository.findById(itemId)
                .filter(i -> i.getUser().getId().equals(userId))
                .ifPresent(cartItemRepository::delete);
    }

    @Transactional
    public void clearCart(Integer userId) {
        cartItemRepository.deleteByUserId(userId);
    }
}
