package sneak_shop.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.dto.request.LoginRequest;
import sneak_shop.dto.request.RegisterRequest;
import sneak_shop.dto.response.AuthResponse;
import sneak_shop.enums.Role;
import sneak_shop.entity.UserEntity;
import sneak_shop.repository.UserRepository;
import sneak_shop.service.AuthService;

import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
			throw new AppException(ErrorCode.CONFLICT, "So dien thoai da ton tai");
		}

		UserEntity user = new UserEntity(
				request.fullName(),
				request.phoneNumber(),
				passwordEncoder.encode(request.password()),
				Set.of(Role.CUSTOMER)
		);

		return AuthResponse.from(userRepository.save(user));
	}

	@Override
	public AuthResponse login(LoginRequest request) {
		UserEntity user = userRepository.findByPhoneNumber(request.phoneNumber())
				.orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "So dien thoai hoac password khong dung"));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new AppException(ErrorCode.UNAUTHORIZED, "So dien thoai hoac password khong dung");
		}

		if (!user.isEnabled()) {
			throw new AppException(ErrorCode.UNAUTHORIZED, "Tai khoan da bi khoa");
		}

		return AuthResponse.from(user);
	}
}
