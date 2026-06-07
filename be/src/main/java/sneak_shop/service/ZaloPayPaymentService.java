package sneak_shop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.entity.OrderEntity;
import sneak_shop.entity.OrderItemEntity;
import sneak_shop.entity.UserEntity;
import sneak_shop.enums.OrderStatus;
import sneak_shop.enums.PaymentStatus;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ZaloPayPaymentService {

    private final RestClient restClient;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantColorRepository colorRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.zalopay.app-id:2553}")
    private String appId;

    @Value("${app.zalopay.key1:PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL}")
    private String key1;

    @Value("${app.zalopay.key2:kLtgPl8HHhfvMuDHPwKfgfsY4Ydm9eIz}")
    private String key2;

    @Value("${app.zalopay.mock-enabled:false}")
    private boolean mockEnabled;

    @Value("${app.zalopay.payment-url:https://sb-openapi.zalopay.vn/v2/create}")
    private String paymentUrl;

    @Value("${app.zalopay.return-url:http://localhost:8080/api/payments/zalopay/return}")
    private String returnUrl;

    @Value("${app.zalopay.callback-url:http://localhost:8080/api/payments/zalopay/callback}")
    private String callbackUrl;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${app.backend.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    public ZaloPayPaymentService(OrderRepository orderRepository,
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
        if (isBlank(key1)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "ZaloPay chua duoc cau hinh");
        }

        long amount = order.getTotalAmount().setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        long appTime = System.currentTimeMillis();
        String appTransId = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "_" + order.getOrderCode();
        String embedData = "{\"redirecturl\":\"" + returnUrl + "?orderCode=" + order.getOrderCode() + "\"}";
        String itemJson = "[]";

        String hmacInput = appId + "|" + appTransId + "|" + user.getFullName() + "|" + amount + "|" + appTime + "|" + embedData + "|" + itemJson;
        String mac = hmacSha256(hmacInput, key1);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("app_id", Integer.parseInt(appId));
        payload.put("app_trans_id", appTransId);
        payload.put("app_user", user.getFullName());
        payload.put("app_time", appTime);
        payload.put("amount", amount);
        payload.put("item", itemJson);
        payload.put("embed_data", embedData);
        payload.put("description", "Thanh toan don hang " + order.getOrderCode());
        payload.put("bank_code", "");
        payload.put("callback_url", callbackUrl);
        payload.put("mac", mac);

        Map<?, ?> response = restClient.post()
                .uri(paymentUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "ZaloPay khong tra ve du lieu");
        }

        int returnCode = number(response.get("return_code")).intValue();
        if (returnCode != 1) {
            String msg = string(response.get("return_message"));
            throw new AppException(ErrorCode.INVALID_REQUEST, msg != null ? msg : "Tao link thanh toan ZaloPay that bai");
        }

        String orderUrl = string(response.get("order_url"));
        if (isBlank(orderUrl)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "ZaloPay khong tra ve order_url");
        }

        transactionRepository.findByOrderId(order.getId()).stream()
                .filter(t -> t.getStatus() == TransactionStatus.pending)
                .findFirst()
                .ifPresent(tx -> {
                    tx.setTransactionCode(appTransId);
                    transactionRepository.save(tx);
                });

        return orderUrl;
    }

    public String buildMockPaymentUrl(String orderCode) {
        return backendBaseUrl.replaceAll("/+$", "") + "/api/payments/zalopay/mock?orderCode=" + orderCode;
    }

    public String renderMockPaymentPage(String orderCode) {
        OrderEntity order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Don hang khong ton tai"));
        String amount = order.getTotalAmount() != null
                ? order.getTotalAmount().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0";
        String safeCode = escapeHtml(order.getOrderCode());
        String safeAmount = escapeHtml(amount);
        String safeReturn = escapeHtml(buildFrontendReturnUrl(order.getOrderCode()));

        return """
                <!doctype html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
                  <title>ZaloPay giả lập</title>
                  <style>
                    body{font-family:Inter,system-ui,-apple-system,sans-serif;background:#e8f4ff;margin:0;color:#111827;}
                    .wrap{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px;}
                    .card{width:min(560px,100%%);background:#fff;border-radius:20px;box-shadow:0 12px 40px rgba(15,23,42,.08);padding:32px;}
                    .brand{display:flex;align-items:center;gap:12px;margin-bottom:18px;}
                    .logo{width:44px;height:44px;border-radius:14px;background:#0068ff;color:#fff;display:grid;place-items:center;font-weight:800;font-size:10px;text-align:center;line-height:1.3;padding:4px;}
                    h1{margin:0 0 10px;font-size:26px;}
                    p{margin:0 0 12px;line-height:1.6;color:#4b5563;}
                    .meta{background:#eff6ff;border:1px solid #bfdbfe;border-radius:14px;padding:16px;margin:18px 0;}
                    .row{display:flex;justify-content:space-between;gap:12px;margin:8px 0;font-size:14px;}
                    .row strong{color:#111827;}
                    form{display:flex;gap:12px;flex-wrap:wrap;margin-top:20px;}
                    button{border:none;border-radius:14px;padding:14px 20px;font-weight:700;cursor:pointer;font-size:14px;}
                    .ok{background:#0068ff;color:#fff;}
                    .fail{background:#e5e7eb;color:#111827;}
                    .hint{margin-top:16px;font-size:13px;color:#6b7280;}
                    .link{color:#0f766e;word-break:break-all;}
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="card">
                      <div class="brand">
                        <div class="logo">Zalo Pay</div>
                        <div>
                          <div style="font-weight:800">ZaloPay giả lập</div>
                          <div style="font-size:12px;color:#6b7280">Dùng để test local</div>
                        </div>
                      </div>
                      <h1>Thanh toán đơn %s</h1>
                      <p>Đây là màn giả lập. Bấm một nút để mô phỏng kết quả thanh toán và quay lại trang đơn hàng.</p>
                      <div class="meta">
                        <div class="row"><span>Mã đơn</span><strong>%s</strong></div>
                        <div class="row"><span>Số tiền</span><strong>%s VND</strong></div>
                      </div>
                      <form method="post" action="/api/payments/zalopay/mock/confirm">
                        <input type="hidden" name="orderCode" value="%s"/>
                        <button class="ok" type="submit" name="success" value="true">Thanh toán thành công</button>
                        <button class="fail" type="submit" name="success" value="false">Thanh toán thất bại</button>
                      </form>
                      <div class="hint">
                        Sau khi bấm, hệ thống sẽ quay lại:<br/>
                        <div class="link">%s</div>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(safeCode, safeCode, safeAmount, safeCode, safeReturn);
    }

    public String handleMockPayment(String orderCode, boolean success) {
        OrderEntity order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Don hang khong ton tai"));
        if (order.getStatus() != OrderStatus.cancelled) {
            if (success) {
                if (order.getPaymentStatus() != PaymentStatus.paid) {
                    order.setPaymentStatus(PaymentStatus.paid);
                    order.setPaidAt(Instant.now());
                    orderRepository.save(order);
                    updateTransactions(order.getId(), TransactionStatus.success, "MOCK-ZLP-" + orderCode);
                }
            } else {
                if (order.getPaymentStatus() == PaymentStatus.pending) {
                    restoreStock(order);
                    order.setPaymentStatus(PaymentStatus.failed);
                    orderRepository.save(order);
                    updateTransactions(order.getId(), TransactionStatus.failed, "MOCK-ZLP-" + orderCode);
                }
            }
        }
        return buildFrontendReturnUrl(orderCode);
    }

    public boolean handleCallback(Map<String, ?> payload) {
        if (payload == null) return false;
        String data = string(payload.get("data"));
        String requestMac = string(payload.get("mac"));
        if (data == null || requestMac == null) return false;
        if (!Objects.equals(hmacSha256(data, key2), requestMac)) return false;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = objectMapper.readValue(data, Map.class);
            String appTransId = string(dataMap.get("app_trans_id"));
            if (appTransId == null) return false;
            String[] parts = appTransId.split("_", 2);
            String orderCode = parts.length == 2 ? parts[1] : appTransId;
            OrderEntity order = orderRepository.findByOrderCode(orderCode).orElse(null);
            if (order == null || order.getStatus() == OrderStatus.cancelled) return true;
            if (order.getPaymentStatus() != PaymentStatus.paid) {
                order.setPaymentStatus(PaymentStatus.paid);
                order.setPaidAt(Instant.now());
                orderRepository.save(order);
                updateTransactions(order.getId(), TransactionStatus.success, appTransId);
            }
        } catch (Exception e) {
            return false;
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
                        tx.setDescription(base.isBlank() ? "ZaloPay transId: " + transId : base + " | ZaloPay transId: " + transId);
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

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Khong the ky giao dich ZaloPay");
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String string(Object value) { return value == null ? null : value.toString(); }

    private Number number(Object value) {
        if (value instanceof Number n) return n;
        if (value == null) return 0;
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
