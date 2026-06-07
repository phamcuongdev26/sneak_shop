package sneak_shop.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sneak_shop.service.ZaloPayPaymentService;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/zalopay")
public class ZaloPayController {

    private final ZaloPayPaymentService zaloPayService;

    public ZaloPayController(ZaloPayPaymentService zaloPayService) {
        this.zaloPayService = zaloPayService;
    }

    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(@RequestBody Map<String, Object> payload) {
        boolean ok = zaloPayService.handleCallback(payload);
        return ResponseEntity.ok(Map.of("return_code", ok ? 1 : -1, "return_message", ok ? "success" : "failed"));
    }

    @GetMapping(value = "/mock", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> mock(@RequestParam String orderCode) {
        return ResponseEntity.ok(zaloPayService.renderMockPaymentPage(orderCode));
    }

    @PostMapping("/mock/confirm")
    public void mockConfirm(@RequestParam String orderCode,
                            @RequestParam boolean success,
                            HttpServletResponse response) throws IOException {
        String redirect = zaloPayService.handleMockPayment(orderCode, success);
        response.sendRedirect(redirect);
    }

    @GetMapping("/return")
    public void returnUrl(@RequestParam(required = false) String orderCode,
                          HttpServletResponse response) throws IOException {
        String redirect = orderCode != null && !orderCode.isBlank()
                ? zaloPayService.buildFrontendReturnUrl(orderCode)
                : zaloPayService.buildFrontendReturnUrl("");
        response.sendRedirect(redirect);
    }
}
