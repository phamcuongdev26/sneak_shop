package sneak_shop.service;

import sneak_shop.dto.request.LoginRequest;
import sneak_shop.dto.request.RegisterEmailRequest;
import sneak_shop.dto.request.PhoneRegisterRequest;
import sneak_shop.dto.request.RegisterRequest;
import sneak_shop.dto.response.AuthResponse;

public interface AuthService {

	AuthResponse register(RegisterRequest request);

	AuthResponse registerEmail(RegisterEmailRequest request);

	AuthResponse registerPhone(PhoneRegisterRequest request);

	void verifyPhoneOtp(String phone, String otp);

	void sendEmailVerificationOtp(String email);

	void verifyEmailVerificationOtp(String email, String otp);

	AuthResponse login(LoginRequest request);

	AuthResponse googleLogin(String idToken);

	AuthResponse zaloLogin(String code, String codeVerifier);

	AuthResponse me(Integer userId);
}
