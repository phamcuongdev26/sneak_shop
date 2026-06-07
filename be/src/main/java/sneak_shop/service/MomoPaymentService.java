package sneak_shop.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.entity.OrderEntity;
import sneak_shop.entity.OrderItemEntity;
import sneak_shop.entity.UserEntity;
import sneak_shop.entity.TransactionEntity;
import sneak_shop.enums.OrderStatus;
import sneak_shop.enums.TransactionStatus;
import sneak_shop.repository.OrderRepository;
import sneak_shop.repository.OrderItemRepository;
import sneak_shop.repository.ProductRepository;
import sneak_shop.repository.ProductVariantColorRepository;
import sneak_shop.repository.TransactionRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class MomoPaymentService {

    private final RestClient restClient;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantColorRepository colorRepository;
    private final TransactionRepository transactionRepository;

    @Value("${app.momo.partner-code:MOMO}")
    private String partnerCode;

    @Value("${app.momo.mock-enabled:false}")
    private boolean mockEnabled;

    @Value("${app.momo.access-key:}")
    private String accessKey;

    @Value("${app.momo.secret-key:}")
    private String secretKey;

    @Value("${app.momo.payment-url:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String paymentUrl;

    @Value("${app.momo.return-url:http://localhost:8080/api/payments/momo/return}")
    private String returnUrl;

    @Value("${app.momo.ipn-url:http://localhost:8080/api/payments/momo/ipn}")
    private String ipnUrl;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${app.backend.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    public MomoPaymentService(OrderRepository orderRepository,
                              OrderItemRepository orderItemRepository,
                              ProductRepository productRepository,
                              ProductVariantColorRepository colorRepository,
                              TransactionRepository transactionRepository) {
        this.restClient = RestClient.builder().build();
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.colorRepository = colorRepository;
        this.transactionRepository = transactionRepository;
    }

    public String createPaymentUrl(OrderEntity order, UserEntity user) {
        if (mockEnabled) {
            return buildMockPaymentUrl(order.getOrderCode());
        }
        if (isBlank(accessKey) || isBlank(secretKey)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "MoMo chua duoc cau hinh");
        }

        List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());
        String requestId = "MOMO-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
        String orderInfo = "Thanh toan don hang " + order.getOrderCode();
        String extraData = Base64.getEncoder().encodeToString(
                ("{\"orderCode\":\"" + order.getOrderCode() + "\",\"userId\":" + user.getId() + "}").getBytes(StandardCharsets.UTF_8)
        );
        long amount = order.getTotalAmount().setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", partnerCode);
        payload.put("accessKey", accessKey);
        payload.put("requestId", requestId);
        payload.put("amount", amount);
        payload.put("orderId", order.getOrderCode());
        payload.put("orderInfo", orderInfo);
        payload.put("redirectUrl", returnUrl);
        payload.put("ipnUrl", ipnUrl);
        payload.put("requestType", "payWithMethod");
        payload.put("extraData", extraData);
        payload.put("autoCapture", true);
        payload.put("lang", "vi");
        payload.put("items", buildItems(items));
        payload.put("userInfo", buildUserInfo(user));
        payload.put("signature", signCreateRequest(payload));

        Map<?, ?> response = restClient.post()
                .uri(paymentUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "MoMo khong tra ve du lieu");
        }

        int resultCode = number(response.get("resultCode")).intValue();
        String message = string(response.get("message"));
        if (resultCode != 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, message != null ? message : "Tao link thanh toan MoMo that bai");
        }

        String payUrl = string(response.get("payUrl"));
        if (isBlank(payUrl)) {
            payUrl = string(response.get("deeplink"));
        }
        if (isBlank(payUrl)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "MoMo khong tra ve payment url");
        }

        transactionRepository.findByOrderId(order.getId()).stream()
                .filter(t -> t.getStatus() == TransactionStatus.pending)
                .findFirst()
                .ifPresent(tx -> {
                    tx.setTransactionCode(requestId);
                    transactionRepository.save(tx);
                });

        return payUrl;
    }

    public String buildMockPaymentUrl(String orderCode) {
        return backendBaseUrl.replaceAll("/+$", "")
                + "/api/payments/momo/mock?orderCode="
                + orderCode;
    }

    public String renderMockPaymentPage(String orderCode) {
        OrderEntity order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Don hang khong ton tai"));
        String amount = order.getTotalAmount() != null ? order.getTotalAmount().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0";
        String title = "MoMo thanh toan gia lap";
        String safeOrderCode = escapeHtml(order.getOrderCode());
        String safeAmount = escapeHtml(amount);
        String safeReturnUrl = escapeHtml(buildFrontendReturnUrl(order.getOrderCode()));
        return """
                <!doctype html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>%s</title>
                  <style>
                    body { font-family: Inter, system-ui, -apple-system, BlinkMacSystemFont, sans-serif; background:#f6f7fb; margin:0; color:#111827; }
                    .wrap { min-height:100vh; display:flex; align-items:center; justify-content:center; padding:24px; }
                    .card { width:min(560px,100%%); background:#fff; border-radius:20px; box-shadow:0 12px 40px rgba(15,23,42,.08); padding:32px; }
                    .brand { display:flex; align-items:center; gap:12px; margin-bottom:18px; }
                    .logo { width:44px; height:44px; border-radius:14px; background:#d82d8b; color:#fff; display:grid; place-items:center; font-weight:800; }
                    h1 { margin:0 0 10px; font-size:28px; }
                    p { margin:0 0 12px; line-height:1.6; color:#4b5563; }
                    .meta { background:#f9fafb; border:1px solid #e5e7eb; border-radius:14px; padding:16px; margin:18px 0; }
                    .row { display:flex; justify-content:space-between; gap:12px; margin:8px 0; font-size:14px; }
                    .row strong { color:#111827; }
                    form { display:flex; gap:12px; flex-wrap:wrap; margin-top:20px; }
                    button { border:none; border-radius:14px; padding:14px 18px; font-weight:700; cursor:pointer; }
                    .ok { background:#d82d8b; color:#fff; }
                    .fail { background:#e5e7eb; color:#111827; }
                    .hint { margin-top:16px; font-size:13px; color:#6b7280; }
                    .link { color:#0f766e; word-break:break-all; }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="card">
                      <div class="brand">
                        <div class="logo">M</div>
                        <div>
                          <div style="font-weight:800">MoMo giả lập</div>
                          <div style="font-size:12px;color:#6b7280">Dùng để test local</div>
                        </div>
                      </div>
                      <h1>Thanh toán đơn %s</h1>
                      <p>Đây là màn giả lập. Bấm một nút để mô phỏng kết quả thanh toán và quay lại trang đơn hàng.</p>
                      <div class="meta">
                        <div class="row"><span>Mã đơn</span><strong>%s</strong></div>
                        <div class="row"><span>Số tiền</span><strong>%s VND</strong></div>
                      </div>
                      <form method="post" action="/api/payments/momo/mock/confirm">
                        <input type="hidden" name="orderCode" value="%s" />
                        <button class="ok" type="submit" name="success" value="true">Thanh toán thành công</button>
                        <button class="fail" type="submit" name="success" value="false">Thanh toán thất bại</button>
                      </form>
                      <div class="hint">
                        Sau khi bấm, hệ thống sẽ quay lại:
                        <div class="link">%s</div>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                title,
                safeOrderCode,
                safeOrderCode,
                safeAmount,
                safeOrderCode,
                safeReturnUrl
        );
    }

    public String handleMockPayment(String orderCode, boolean success) {
        OrderEntity order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Don hang khong ton tai"));
        if (order.getStatus() != OrderStatus.cancelled) {
            if (success) {
                if (order.getPaymentStatus() != sneak_shop.enums.PaymentStatus.paid) {
                    order.setPaymentStatus(sneak_shop.enums.PaymentStatus.paid);
                    order.setPaidAt(Instant.now());
                    orderRepository.save(order);
                    updateTransactions(order.getId(), TransactionStatus.success, "MOCK-" + orderCode);
                }
            } else {
                if (order.getPaymentStatus() == sneak_shop.enums.PaymentStatus.pending) {
                    restoreStock(order);
                    order.setPaymentStatus(sneak_shop.enums.PaymentStatus.failed);
                    orderRepository.save(order);
                    updateTransactions(order.getId(), TransactionStatus.failed, "MOCK-" + orderCode);
                }
            }
        }
        return buildFrontendReturnUrl(orderCode);
    }

    public boolean handleIpn(Map<String, ?> payload) {
        if (payload == null || payload.isEmpty()) return false;
        if (!verifyIpnSignature(payload)) {
            return false;
        }

        String orderCode = string(payload.get("orderId"));
        OrderEntity order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) return false;

        long amount = number(payload.get("amount")).longValue();
        long expected = order.getTotalAmount().setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        if (amount != expected) {
            return false;
        }

        if (order.getStatus() == OrderStatus.cancelled) {
            return true;
        }

        int resultCode = number(payload.get("resultCode")).intValue();
        if (resultCode == 0 || resultCode == 9000) {
            if (order.getPaymentStatus() != sneak_shop.enums.PaymentStatus.paid) {
                order.setPaymentStatus(sneak_shop.enums.PaymentStatus.paid);
                order.setPaidAt(Instant.now());
                orderRepository.save(order);
                updateTransactions(order.getId(), TransactionStatus.success, string(payload.get("transId")));
            }
            return true;
        }

        if (order.getPaymentStatus() == sneak_shop.enums.PaymentStatus.pending
                && order.getStatus() != sneak_shop.enums.OrderStatus.cancelled) {
            restoreStock(order);
            order.setPaymentStatus(sneak_shop.enums.PaymentStatus.failed);
            orderRepository.save(order);
            updateTransactions(order.getId(), TransactionStatus.failed, string(payload.get("transId")));
        }
        return true;
    }

    public String buildFrontendReturnUrl(String orderCode) {
        return frontendBaseUrl.replaceAll("/+$", "") + "/orders/" + orderCode;
    }

    private void updateTransactions(Integer orderId, TransactionStatus status, String transId) {
        transactionRepository.findByOrderId(orderId).stream()
                .filter(t -> t.getStatus() == TransactionStatus.pending || t.getStatus() == status)
                .findFirst()
                .ifPresent(tx -> {
                    tx.setStatus(status);
                    if (transId != null && !transId.isBlank()) {
                        String base = tx.getDescription() != null ? tx.getDescription() : "";
                        tx.setDescription(base.isBlank() ? "MoMo transId: " + transId : base + " | MoMo transId: " + transId);
                    }
                    transactionRepository.save(tx);
                });
    }

    private void restoreStock(OrderEntity order) {
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItemEntity item : items) {
            if (item.getColor() != null) {
                colorRepository.addStock(item.getColor().getId(), item.getQuantity());
            } else if (item.getVariant() == null && item.getProduct() != null && item.getProduct().getStockQuantity() != null) {
                productRepository.addStock(item.getProduct().getId(), item.getQuantity());
            }
        }
    }

    private Map<String, Object> buildUserInfo(UserEntity user) {
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("name", user.getFullName());
        userInfo.put("phoneNumber", user.getPhone());
        userInfo.put("email", user.getEmail());
        return userInfo;
    }

    private List<Map<String, Object>> buildItems(List<OrderItemEntity> items) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (OrderItemEntity item : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getProduct() != null ? item.getProduct().getId() : item.getId());
            row.put("name", item.getProductName());
            row.put("description", item.getVariantName() != null ? item.getVariantName() : item.getColorName());
            row.put("category", item.getProduct() != null && item.getProduct().getName() != null ? item.getProduct().getName() : "San pham");
            row.put("imageUrl", item.getProductImage());
            row.put("quantity", item.getQuantity());
            row.put("price", item.getFinalPrice().setScale(0, java.math.RoundingMode.HALF_UP).longValueExact());
            row.put("totalPrice", item.getSubtotal().setScale(0, java.math.RoundingMode.HALF_UP).longValueExact());
            result.add(row);
        }
        return result;
    }

    private String signCreateRequest(Map<String, Object> payload) {
        String raw = "accessKey=" + accessKey
                + "&amount=" + payload.get("amount")
                + "&extraData=" + payload.get("extraData")
                + "&ipnUrl=" + payload.get("ipnUrl")
                + "&orderId=" + payload.get("orderId")
                + "&orderInfo=" + payload.get("orderInfo")
                + "&partnerCode=" + payload.get("partnerCode")
                + "&redirectUrl=" + payload.get("redirectUrl")
                + "&requestId=" + payload.get("requestId")
                + "&requestType=" + payload.get("requestType");
        return hmacSha256(raw, secretKey);
    }

    private boolean verifyIpnSignature(Map<String, ?> payload) {
        String raw = "accessKey=" + string(payload.get("accessKey"))
                + "&amount=" + payload.get("amount")
                + "&extraData=" + string(payload.get("extraData"))
                + "&message=" + string(payload.get("message"))
                + "&orderId=" + string(payload.get("orderId"))
                + "&orderInfo=" + string(payload.get("orderInfo"))
                + "&orderType=" + string(payload.get("orderType"))
                + "&partnerCode=" + string(payload.get("partnerCode"))
                + "&payType=" + string(payload.get("payType"))
                + "&requestId=" + string(payload.get("requestId"))
                + "&responseTime=" + string(payload.get("responseTime"))
                + "&resultCode=" + payload.get("resultCode")
                + "&transId=" + payload.get("transId");
        return Objects.equals(hmacSha256(raw, secretKey), string(payload.get("signature")));
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(rawHmac.length * 2);
            for (byte b : rawHmac) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Khong the ky giao dich MoMo");
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    private Number number(Object value) {
        if (value instanceof Number n) return n;
        if (value == null) return 0;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
