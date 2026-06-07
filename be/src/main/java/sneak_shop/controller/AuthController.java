package sneak_shop.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.dto.request.*;
import sneak_shop.dto.response.AuthResponse;
import sneak_shop.security.UserContext;
import sneak_shop.service.AuthService;
import sneak_shop.service.PasswordResetService;
import sneak_shop.service.PhoneOtpService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final PhoneOtpService phoneOtpService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService,
                          PhoneOtpService phoneOtpService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.phoneOtpService = phoneOtpService;
    }

    // ── Email register ──────────────────────────────────────────────────────

    @PostMapping("/send-register-otp")
    public ApiResponse<Void> sendRegisterOtp(@RequestBody Map<String, String> body) {
        passwordResetService.sendRegisterOtp(body.get("email"));
        return ApiResponse.ok("Mã OTP đã được gửi vào email của bạn");
    }

    @PostMapping("/verify-register-otp")
    public ApiResponse<Void> verifyRegisterOtp(@RequestBody Map<String, String> body) {
        passwordResetService.verifyRegisterOtp(body.get("email"), body.get("otp"));
        return ApiResponse.ok("Xác thực OTP thành công");
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Đăng ký thành công", authService.register(request));
    }

    @PostMapping("/register-email")
    public ApiResponse<AuthResponse> registerEmail(@Valid @RequestBody RegisterEmailRequest request) {
        return ApiResponse.ok("Đăng ký thành công", authService.registerEmail(request));
    }

    // ── Phone register ──────────────────────────────────────────────────────

    @PostMapping("/send-phone-otp")
    public ApiResponse<Void> sendPhoneOtp(@RequestBody Map<String, String> body) {
        phoneOtpService.sendOtp(body.get("phone"));
        return ApiResponse.ok("Mã OTP đã được gửi qua Zalo");
    }

    @PostMapping("/send-email-verification-otp")
    public ApiResponse<Void> sendEmailVerificationOtp(@RequestBody Map<String, String> body) {
        authService.sendEmailVerificationOtp(body.get("email"));
        return ApiResponse.ok("Mã OTP đã được gửi vào email của bạn");
    }

    @PostMapping("/verify-phone-otp")
    public ApiResponse<Void> verifyPhoneOtp(@Valid @RequestBody VerifyPhoneOtpRequest req) {
        authService.verifyPhoneOtp(req.phone(), req.otp());
        return ApiResponse.ok("Xác thực OTP thành công");
    }

    @PostMapping("/verify-email-otp")
    public ApiResponse<Void> verifyEmailOtp(@Valid @RequestBody VerifyEmailOtpRequest req) {
        authService.verifyEmailVerificationOtp(req.email(), req.otp());
        return ApiResponse.ok("Xác thực email thành công");
    }

    @PostMapping("/register-phone")
    public ApiResponse<AuthResponse> registerPhone(@Valid @RequestBody PhoneRegisterRequest request) {
        return ApiResponse.ok("Đăng ký thành công", authService.registerPhone(request));
    }

    /** Dev-only endpoint: get current OTP for a phone (only in mock mode) */
    @GetMapping("/dev/phone-otp")
    public ApiResponse<Object> devPhoneOtp(@RequestParam String phone) {
        String otp = phoneOtpService.getDevOtp(phone);
        if (otp == null) return ApiResponse.ok((Object) null);
        return ApiResponse.ok((Object) otp);
    }

    // ── Login ───────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Đăng nhập thành công", authService.login(request));
    }

    @PostMapping("/google")
    public ApiResponse<AuthResponse> googleLogin(@Valid @RequestBody GoogleAuthRequest req) {
        return ApiResponse.ok("Đăng nhập thành công", authService.googleLogin(req.idToken()));
    }

    @PostMapping("/zalo-login")
    public ApiResponse<AuthResponse> zaloLogin(@RequestBody Map<String, String> body) {
        return ApiResponse.ok("Đăng nhập thành công",
                authService.zaloLogin(body.get("code"), body.get("codeVerifier")));
    }

    // ── Password reset ──────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        passwordResetService.sendOtp(req.email(), req.phone(), req.fullName());
        return ApiResponse.ok("Mã OTP đã được gửi vào email của bạn");
    }

    @PostMapping("/verify-otp")
    public ApiResponse<String> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        String token = passwordResetService.verifyOtp(req.email(), req.otp());
        return ApiResponse.ok("Xác nhận thành công", token);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.token(), req.newPassword());
        return ApiResponse.ok("Đổi mật khẩu thành công");
    }

    // ── Me ──────────────────────────────────────────────────────────────────

    @GetMapping("/me")
    public ApiResponse<AuthResponse> me(@AuthenticationPrincipal UserContext ctx) {
        if (ctx == null) throw new AppException(ErrorCode.UNAUTHORIZED, "Ban chua dang nhap");
        return ApiResponse.ok(authService.me(ctx.id()));
    }
}
