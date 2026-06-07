package sneak_shop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PhoneOtpService {

    private static final Logger log = LoggerFactory.getLogger(PhoneOtpService.class);

    private record OtpEntry(String otp, Instant expiresAt) {}
    private record VerifiedEntry(Instant expiresAt) {}

    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();
    private final Map<String, VerifiedEntry> verifiedStore = new ConcurrentHashMap<>();
    private final RestClient restClient = RestClient.builder().build();

    @Value("${app.phone-otp.mock-enabled:true}")
    private boolean mockEnabled;

    @Value("${app.phone-otp.zalo.access-token:}")
    private String zaloAccessToken;

    @Value("${app.phone-otp.zalo.template-id:}")
    private String zaloTemplateId;

    public void sendOtp(String phone) {
        String normalizedPhone = normalizePhone(phone);
        String otp = String.format("%06d", (int) (Math.random() * 1_000_000));
        store.put(normalizedPhone, new OtpEntry(otp, Instant.now().plusSeconds(300)));
        verifiedStore.remove(normalizedPhone);

        if (mockEnabled) {
            log.info("=== PHONE OTP MOCK [{}] === OTP: {} ===", normalizedPhone, otp);
            return;
        }

        sendViaZalo(normalizedPhone, otp);
    }

    /** Keep backward-compat for callers that still pass channel arg */
    public void sendOtp(String phone, String ignoredChannel) {
        sendOtp(phone);
    }

    public void verifyOtp(String phone, String otp) {
        String normalizedPhone = normalizePhone(phone);
        OtpEntry entry = store.get(normalizedPhone);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP đã hết hạn, vui lòng thử lại");
        }
        if (!entry.otp().equals(otp)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP không đúng");
        }
        store.remove(normalizedPhone);
        verifiedStore.put(normalizedPhone, new VerifiedEntry(Instant.now().plusSeconds(600)));
    }

    public void verifyAndMark(String phone, String otp) {
        verifyOtp(phone, otp);
    }

    public void consumeVerifiedPhone(String phone) {
        String normalizedPhone = normalizePhone(phone);
        VerifiedEntry entry = verifiedStore.get(normalizedPhone);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Số điện thoại chưa được xác thực OTP");
        }
        verifiedStore.remove(normalizedPhone);
    }

    /** Dev-only: get current OTP for a phone (only works in mock mode) */
    public String getDevOtp(String phone) {
        if (!mockEnabled) throw new AppException(ErrorCode.INVALID_REQUEST, "Chi ho tro trong mock mode");
        OtpEntry entry = store.get(normalizePhone(phone));
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) return null;
        return entry.otp();
    }

    private void sendViaZalo(String phone, String otp) {
        if (zaloAccessToken.isBlank() || zaloTemplateId.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Zalo ZNS chua duoc cau hinh");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("phone", phone);
        body.put("template_id", zaloTemplateId);
        body.put("template_data", Map.of("otp", otp));
        body.put("tracking_id", "SNEAKSHOP-OTP-" + phone);

        try {
            Map<?, ?> resp = restClient.post()
                    .uri("https://business.openapi.zalo.me/message/template")
                    .header("access_token", zaloAccessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (resp == null) throw new AppException(ErrorCode.INVALID_REQUEST, "Zalo khong tra ve du lieu");
            Object error = resp.get("error");
            if (error != null && !error.toString().equals("0")) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Gui Zalo OTP that bai: " + resp.get("message"));
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Loi gui OTP qua Zalo: " + e.getMessage());
        }
    }

    public String normalizePhone(String phone) {
        if (phone == null) return phone;
        phone = phone.replaceAll("[\\s\\-]", "");
        if (phone.startsWith("0")) return "84" + phone.substring(1);
        if (phone.startsWith("+84")) return phone.substring(1);
        return phone;
    }
}
