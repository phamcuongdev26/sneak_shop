package sneak_shop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;

import java.util.Map;

@Slf4j
@Service
public class ZaloAuthService {

    @Value("${app.zalo-auth.app-id:}")
    private String appId;

    @Value("${app.zalo-auth.secret-key:}")
    private String secretKey;

    @Value("${app.zalo-auth.redirect-uri:http://localhost:3000/zalo/callback}")
    private String redirectUri;

    private final RestTemplate rest = new RestTemplate();

    public record ZaloUserInfo(String id, String name, String avatarUrl) {}

    @SuppressWarnings("unchecked")
    public ZaloUserInfo getUserInfo(String code, String codeVerifier) {
        // Exchange code for access token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("secret_key", secretKey);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("app_id", appId);
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("code_verifier", codeVerifier);

        Map<String, Object> tokenRes;
        try {
            ResponseEntity<Map> resp = rest.exchange(
                "https://oauth.zaloapp.com/v4/access_token",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
            );
            tokenRes = (Map<String, Object>) resp.getBody();
        } catch (Exception e) {
            log.error("Zalo token exchange failed: {}", e.getMessage());
            throw new AppException(ErrorCode.UNAUTHORIZED, "Không thể xác thực với Zalo");
        }

        if (tokenRes == null || tokenRes.containsKey("error")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Token Zalo không hợp lệ");
        }

        String accessToken = (String) tokenRes.get("access_token");

        // Get user info
        Map<String, Object> userRes;
        try {
            ResponseEntity<Map> resp = rest.getForEntity(
                "https://graph.zalo.me/v2.0/me?access_token=" + accessToken + "&fields=id,name,picture",
                Map.class
            );
            userRes = (Map<String, Object>) resp.getBody();
        } catch (Exception e) {
            log.error("Zalo user info fetch failed: {}", e.getMessage());
            throw new AppException(ErrorCode.UNAUTHORIZED, "Không thể lấy thông tin người dùng từ Zalo");
        }

        if (userRes == null || userRes.containsKey("error")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Không thể lấy thông tin người dùng từ Zalo");
        }

        String zaloId = String.valueOf(userRes.get("id"));
        String name = (String) userRes.getOrDefault("name", "Zalo User");

        String avatar = null;
        try {
            Map<String, Object> picture = (Map<String, Object>) userRes.get("picture");
            if (picture != null) {
                Map<String, Object> data = (Map<String, Object>) picture.get("data");
                if (data != null) avatar = (String) data.get("url");
            }
        } catch (Exception ignored) {}

        return new ZaloUserInfo(zaloId, name, avatar);
    }
}
