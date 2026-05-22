package sneak_shop.dto.response;

import sneak_shop.entity.UserEntity;

import java.util.Set;

public record AuthResponse(
		String id,
		String fullName,
		String phoneNumber,
		Set<String> roles,
		Set<String> permissions,
		String tokenType,
		String accessToken
) {

	public static AuthResponse from(UserEntity user) {
		return from(user, null);
	}

	public static AuthResponse from(UserEntity user, String accessToken) {
		return new AuthResponse(
				user.getId(),
				user.getFullName(),
				user.getPhoneNumber(),
				user.getRoles(),
				user.getPermissions(),
				accessToken == null ? null : "Bearer",
				accessToken
		);
	}
}
