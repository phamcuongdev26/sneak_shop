package sneak_shop.dto.response;

import sneak_shop.entity.OrderEntity;
import sneak_shop.entity.OrderItemEntity;
import sneak_shop.entity.TransactionEntity;
import sneak_shop.enums.OrderStatus;
import sneak_shop.enums.PaymentMethod;
import sneak_shop.enums.PaymentStatus;
import sneak_shop.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Integer id,
        String orderCode,
        String recipientName,
        String recipientPhone,
        String shippingAddress,
        String shippingCity,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        OrderStatus status,
        String note,
        String cancelReason,
        Instant paidAt,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemSummary> items,
        List<TransactionSummary> transactions
) {
    public record OrderItemSummary(
            Integer id, Integer productId, Integer variantId, Integer colorId,
            String productName, String variantName, String colorName,
            String sku, String productImage,
            BigDecimal productPrice, Integer discountPercent, BigDecimal finalPrice,
            BigDecimal price, Integer quantity, BigDecimal subtotal
    ) {
        public static OrderItemSummary from(OrderItemEntity e) {
            return new OrderItemSummary(
                    e.getId(),
                    e.getProduct() != null ? e.getProduct().getId() : null,
                    e.getVariant() != null ? e.getVariant().getId() : null,
                    e.getColor() != null ? e.getColor().getId() : null,
                    e.getProductName(), e.getVariantName(), e.getColorName(),
                    e.getSku(), e.getProductImage(),
                    e.getProductPrice(), e.getDiscountPercent(), e.getFinalPrice(),
                    e.getPrice(), e.getQuantity(), e.getSubtotal());
        }
    }

    public record TransactionSummary(
            Integer id, String transactionCode, BigDecimal amount, PaymentMethod paymentMethod,
            TransactionStatus status, String description, Instant createdAt, Instant updatedAt
    ) {
        public static TransactionSummary from(TransactionEntity e) {
            return new TransactionSummary(
                    e.getId(), e.getTransactionCode(), e.getAmount(), e.getPaymentMethod(),
                    e.getStatus(), e.getDescription(), e.getCreatedAt(), e.getUpdatedAt()
            );
        }
    }

    public static OrderResponse from(OrderEntity e, List<OrderItemEntity> items, List<TransactionEntity> transactions) {
        return new OrderResponse(
                e.getId(), e.getOrderCode(), e.getRecipientName(), e.getRecipientPhone(),
                e.getShippingAddress(), e.getShippingCity(),
                e.getSubtotal(), e.getShippingFee(), e.getDiscountAmount(), e.getTotalAmount(),
                e.getPaymentMethod(), e.getPaymentStatus(), e.getStatus(),
                e.getNote(), e.getCancelReason(), e.getPaidAt(), e.getCancelledAt(), e.getCreatedAt(), e.getUpdatedAt(),
                items.stream().map(OrderItemSummary::from).toList(),
                transactions.stream().map(TransactionSummary::from).toList()
        );
    }
}
