package sneak_shop.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.dto.request.LoginRequest;
import sneak_shop.dto.request.RegisterRequest;
import sneak_shop.dto.response.AuthResponse;
import sneak_shop.entity.RoleEntity;
import sneak_shop.entity.UserEntity;
import sneak_shop.repository.RoleRepository;
import sneak_shop.repository.UserRepository;
import sneak_shop.security.JwtService;
import sneak_shop.service.AuthService;

import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthServiceImpl(
			UserRepository userRepository,
			RoleRepository roleRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Override
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
			throw new AppException(ErrorCode.CONFLICT, "So dien thoai da ton tai");
		}
		RoleEntity customerRole = roleRepository.findByName("CUSTOMER")
				.orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Role CUSTOMER chua duoc khoi tao"));

		UserEntity user = new UserEntity(
				request.fullName(),
				request.phoneNumber(),
				passwordEncoder.encode(request.password()),
				Set.of(customerRole)
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

		return AuthResponse.from(user, jwtService.generateAccessToken(user));
	}
}
