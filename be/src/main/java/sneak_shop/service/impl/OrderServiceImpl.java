package sneak_shop.service.impl;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.CheckoutItemRequest;
import sneak_shop.dto.request.CheckoutRequest;
import sneak_shop.dto.request.UpdateOrderStatusRequest;
import sneak_shop.dto.response.CheckoutResponse;
import sneak_shop.dto.response.OrderResponse;
import sneak_shop.entity.*;
import sneak_shop.enums.*;
import sneak_shop.repository.*;
import sneak_shop.service.OrderService;
import sneak_shop.service.MomoPaymentService;
import sneak_shop.service.ZaloPayPaymentService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final NotificationRepository notificationRepository;
    private final ProductVariantColorRepository colorRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final TransactionRepository transactionRepository;
    private final MomoPaymentService momoPaymentService;
    private final ZaloPayPaymentService zaloPayPaymentService;

    public OrderServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                            OrderStatusHistoryRepository historyRepository, CartItemRepository cartItemRepository,
                            UserRepository userRepository, AddressRepository addressRepository,
                            NotificationRepository notificationRepository,
                            ProductVariantColorRepository colorRepository, ProductRepository productRepository,
                            ProductVariantRepository variantRepository,
                            TransactionRepository transactionRepository,
                            MomoPaymentService momoPaymentService,
                            ZaloPayPaymentService zaloPayPaymentService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.historyRepository = historyRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.notificationRepository = notificationRepository;
        this.colorRepository = colorRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.transactionRepository = transactionRepository;
        this.momoPaymentService = momoPaymentService;
        this.zaloPayPaymentService = zaloPayPaymentService;
    }

    private record OrderItem(ProductEntity product, ProductVariantEntity variant,
                             ProductVariantColorEntity color, int quantity) {}

    @Transactional
    public CheckoutResponse checkout(Integer userId, CheckoutRequest req) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User khong ton tai"));

        boolean isBuyNow = req.items() != null && !req.items().isEmpty();
        List<OrderItem> items = isBuyNow
                ? buildBuyNowItems(req.items())
                : buildCartItems(userId);

        if (items.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Gio hang trong");
        }

        for (OrderItem item : items) {
            if (item.color() != null) {
                int stock = item.color().getStockQuantity();
                if (stock < item.quantity())
                    throw new AppException(ErrorCode.INVALID_REQUEST,
                            "San pham \"" + item.product().getName() + "\" - " +
                            item.color().getColor() + " chi con " + stock + " trong kho");
            } else if (item.variant() == null && item.product().getStockQuantity() != null) {
                int stock = item.product().getStockQuantity();
                if (stock < item.quantity())
                    throw new AppException(ErrorCode.INVALID_REQUEST,
                            "San pham \"" + item.product().getName() + "\" chi con " + stock + " trong kho");
            }
        }

        BigDecimal subtotal = items.stream()
                .map(i -> finalItemPrice(i).multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingFee = subtotal.compareTo(new BigDecimal("500000")) >= 0
                ? BigDecimal.ZERO : new BigDecimal("30000");
        BigDecimal total = subtotal.add(shippingFee);

        AddressEntity linkedAddress = null;
        if (req.addressId() != null) {
            linkedAddress = addressRepository.findById(req.addressId())
                    .filter(a -> a.getUser().getId().equals(userId))
                    .orElse(null);
        }

        OrderEntity order = OrderEntity.builder()
                .orderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .user(user).address(linkedAddress)
                .discountAmount(BigDecimal.ZERO)
                .recipientName(req.recipientName()).recipientPhone(req.recipientPhone())
                .shippingAddress(req.shippingAddress()).shippingWard(req.shippingWard())
                .shippingDistrict(req.shippingDistrict()).shippingCity(req.shippingCity())
                .subtotal(subtotal).shippingFee(shippingFee).totalAmount(total)
                .paymentMethod(req.paymentMethod()).note(req.note())
                .status(OrderStatus.pending)
                .build();
        order = orderRepository.save(order);

        boolean isOnlinePayment = req.paymentMethod() == PaymentMethod.bank_transfer
                || req.paymentMethod() == PaymentMethod.e_wallet;
        boolean requiresImmediateStockDeduction = req.paymentMethod() != PaymentMethod.bank_transfer
                && req.paymentMethod() != PaymentMethod.e_wallet;

        for (OrderItem item : items) {
            BigDecimal productPrice = item.variant() != null ? item.variant().getPrice() : item.product().getPrice();
            Integer discountPercent = item.product().getDiscountPercent() != null ? item.product().getDiscountPercent() : 0;
            BigDecimal finalPrice = finalItemPrice(item);
            String variantLabel = item.variant() != null && item.variant().getSize() != null ? "Size " + item.variant().getSize() : null;
            String colorLabel = item.color() != null ? item.color().getColor() : null;
            String imageUrl = item.color() != null && item.color().getImageUrl() != null
                    ? item.color().getImageUrl() : item.product().getCoverImageUrl();

            orderItemRepository.save(OrderItemEntity.builder()
                    .order(order).product(item.product())
                    .variant(item.variant()).color(item.color())
                    .colorName(colorLabel)
                    .sku(item.variant() != null ? item.variant().getSku() : null)
                    .productName(item.product().getName())
                    .variantName(variantLabel)
                    .productImage(imageUrl)
                    .productPrice(productPrice)
                    .discountPercent(discountPercent)
                    .finalPrice(finalPrice)
                    .price(finalPrice).quantity(item.quantity())
                    .subtotal(finalPrice.multiply(BigDecimal.valueOf(item.quantity())))
                    .build());

            if (requiresImmediateStockDeduction) {
                deductStock(item);
            }
        }

        historyRepository.save(OrderStatusHistoryEntity.builder()
                .order(order).toStatus(OrderStatus.pending).note("Don hang duoc tao").build());

        if (!isBuyNow) {
            cartItemRepository.deleteByUserId(userId);
        }

        boolean needsTransaction = isOnlinePayment
                || req.paymentMethod() == PaymentMethod.momo
                || req.paymentMethod() == PaymentMethod.zalopay;
        if (needsTransaction) {
            transactionRepository.save(TransactionEntity.builder()
                    .order(order)
                    .transactionCode("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .amount(total)
                    .paymentMethod(req.paymentMethod())
                    .status(TransactionStatus.pending)
                    .description("Thanh toan don hang " + order.getOrderCode())
                    .build());
        }

        String paymentUrl = null;
        if (req.paymentMethod() == PaymentMethod.momo) {
            paymentUrl = momoPaymentService.createPaymentUrl(order, user);
        } else if (req.paymentMethod() == PaymentMethod.zalopay) {
            paymentUrl = zaloPayPaymentService.createPaymentUrl(order, user);
        }

        return new CheckoutResponse(toResponse(order), paymentUrl);
    }

    private void deductStock(OrderItem item) {
        if (item.color() != null) {
            int updated = colorRepository.deductStock(item.color().getId(), item.quantity());
            if (updated == 0)
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        "San pham \"" + item.product().getName() + "\" - " +
                        item.color().getColor() + " vua het hang");
        } else if (item.variant() == null && item.product().getStockQuantity() != null) {
            int updated = productRepository.deductStock(item.product().getId(), item.quantity());
            if (updated == 0)
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        "San pham \"" + item.product().getName() + "\" vua het hang");
        }
    }

    private List<OrderItem> buildCartItems(Integer userId) {
        return cartItemRepository.findByUserId(userId).stream()
                .map(c -> new OrderItem(c.getProduct(), c.getVariant(), c.getColor(), c.getQuantity()))
                .toList();
    }

    private BigDecimal finalItemPrice(OrderItem item) {
        BigDecimal price = item.variant() != null ? item.variant().getPrice() : item.product().getPrice();
        Integer discountPercent = item.product().getDiscountPercent() != null ? item.product().getDiscountPercent() : 0;
        if (discountPercent <= 0) return price;
        return price.multiply(BigDecimal.valueOf(100L - discountPercent)).divide(BigDecimal.valueOf(100));
    }

    private List<OrderItem> buildBuyNowItems(List<CheckoutItemRequest> requests) {
        List<OrderItem> result = new ArrayList<>();
        for (CheckoutItemRequest r : requests) {
            ProductEntity product = productRepository.findById(r.productId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "San pham khong ton tai"));
            ProductVariantEntity variant = null;
            if (r.variantId() != null) {
                variant = variantRepository.findById(r.variantId())
                        .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Variant khong ton tai"));
            }
            ProductVariantColorEntity color = null;
            if (r.colorId() != null) {
                color = colorRepository.findById(r.colorId())
                        .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Mau sac khong ton tai"));
            }
            result.add(new OrderItem(product, variant, color, r.quantity()));
        }
        return result;
    }

    public PageResponse<OrderResponse> getMyOrders(Integer userId, OrderStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var pageResult = status != null
                ? orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable)
                : orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(pageResult.map(this::toResponse));
    }

    public OrderResponse getMyOrder(Integer userId, String orderCode) {
        OrderEntity order = orderRepository.findByOrderCode(orderCode)
                .filter(o -> o.getUser().getId().equals(userId))
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Don hang khong ton tai"));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Integer userId, String orderCode, String reason) {
        OrderEntity order = orderRepository.findByOrderCode(orderCode)
                .filter(o -> o.getUser().getId().equals(userId))
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Don hang khong ton tai"));

        if (order.getStatus() != OrderStatus.pending
                && order.getStatus() != OrderStatus.confirmed
                && order.getStatus() != OrderStatus.shipping) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Khong the huy don hang o trang thai nay");
        }

        restoreStock(order);
        updateStatus(order, OrderStatus.cancelled, reason);
        order.setCancelReason(reason);
        order.setCancelledAt(Instant.now());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse confirmReceived(Integer userId, String orderCode) {
        OrderEntity order = orderRepository.findByOrderCode(orderCode)
                .filter(o -> o.getUser().getId().equals(userId))
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Don hang khong ton tai"));

        if (order.getStatus() == OrderStatus.completed) {
            return toResponse(order);
        }
        if (order.getStatus() != OrderStatus.delivered) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chi co the xac nhan da nhan hang khi don da giao");
        }

        updateStatus(order, OrderStatus.completed, null);
        order.setCompletedAt(Instant.now());
        if (order.getPaymentMethod() == PaymentMethod.cod && order.getPaymentStatus() == PaymentStatus.pending) {
            order.setPaymentStatus(PaymentStatus.paid);
            order.setPaidAt(Instant.now());
        }
        return toResponse(orderRepository.save(order));
    }

    public PageResponse<OrderResponse> adminGetUserOrders(Integer userId, int page, int size) {
        var pageable = PageRequest.of(page, size);
        return PageResponse.from(
                orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toResponse)
        );
    }

    public PageResponse<OrderResponse> adminGetAll(OrderStatus status, String keyword, int page, int size) {
        var pageable = PageRequest.of(page, size);
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        var pageResult = orderRepository.searchByKeyword(status, kw, pageable);
        return PageResponse.from(pageResult.map(this::toResponse));
    }

    @Transactional
    public OrderResponse adminUpdateStatus(String orderCode, UpdateOrderStatusRequest req) {
        OrderEntity order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Don hang khong ton tai"));
        updateStatus(order, req.status(), null);
        if (req.status() == OrderStatus.confirmed) order.setConfirmedAt(Instant.now());
        if (req.status() == OrderStatus.shipping)  order.setShippingAt(Instant.now());
        if (req.status() == OrderStatus.delivered) order.setDeliveredAt(Instant.now());
        if (req.status() == OrderStatus.completed) {
            order.setCompletedAt(Instant.now());
            if (order.getPaymentMethod() == PaymentMethod.cod && order.getPaymentStatus() == PaymentStatus.pending) {
                order.setPaymentStatus(PaymentStatus.paid);
                order.setPaidAt(Instant.now());
            }
        }
        if (req.status() == OrderStatus.cancelled) {
            order.setCancelledAt(Instant.now());
            order.setCancelReason(req.cancelReason());
            restoreStock(order);
        }
        return toResponse(orderRepository.save(order));
    }

    private void updateStatus(OrderEntity order, OrderStatus newStatus, String note) {
        historyRepository.save(OrderStatusHistoryEntity.builder()
                .order(order).fromStatus(order.getStatus()).toStatus(newStatus).note(note).build());
        order.setStatus(newStatus);
    }

    private OrderResponse toResponse(OrderEntity order) {
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());
        List<TransactionEntity> transactions = transactionRepository.findByOrderId(order.getId());
        return OrderResponse.from(order, items, transactions);
    }

    private void restoreStock(OrderEntity order) {
        boolean isOnlineUnpaid = (order.getPaymentMethod() == PaymentMethod.bank_transfer
                || order.getPaymentMethod() == PaymentMethod.e_wallet)
                && order.getPaymentStatus() != PaymentStatus.paid;
        if (isOnlineUnpaid) return;

        List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItemEntity item : items) {
            if (item.getColor() != null) {
                colorRepository.addStock(item.getColor().getId(), item.getQuantity());
            } else if (item.getVariant() == null && item.getProduct().getStockQuantity() != null) {
                productRepository.addStock(item.getProduct().getId(), item.getQuantity());
            }
        }
    }
}
