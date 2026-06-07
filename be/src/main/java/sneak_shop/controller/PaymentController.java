package sneak_shop.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sneak_shop.service.MomoPaymentService;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/momo")
public class PaymentController {

    private final MomoPaymentService momoPaymentService;

    public PaymentController(MomoPaymentService momoPaymentService) {
        this.momoPaymentService = momoPaymentService;
    }

    @PostMapping("/ipn")
    public ResponseEntity<Void> ipn(@RequestBody Map<String, Object> payload) {
        momoPaymentService.handleIpn(payload);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/mock", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> mock(@RequestParam String orderCode) {
        return ResponseEntity.ok(momoPaymentService.renderMockPaymentPage(orderCode));
    }

    @PostMapping("/mock/confirm")
    public void mockConfirm(@RequestParam String orderCode,
                            @RequestParam boolean success,
                            HttpServletResponse response) throws IOException {
        String redirect = momoPaymentService.handleMockPayment(orderCode, success);
        response.sendRedirect(redirect);
    }

    @GetMapping("/return")
    public void returnUrl(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        String orderCode = params.get("orderId");
        String redirect = orderCode != null && !orderCode.isBlank()
                ? momoPaymentService.buildFrontendReturnUrl(orderCode)
                : momoPaymentService.buildFrontendReturnUrl("");
        response.sendRedirect(redirect);
    }
}
