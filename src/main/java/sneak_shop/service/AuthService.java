package sneak_shop.service;

import sneak_shop.dto.request.LoginRequest;
import sneak_shop.dto.request.RegisterRequest;
import sneak_shop.dto.response.AuthResponse;

public interface AuthService {

	AuthResponse register(RegisterRequest request);

	AuthResponse login(LoginRequest request);
}
