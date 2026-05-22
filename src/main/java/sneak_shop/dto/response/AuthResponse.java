package sneak_shop.dto.response;

import sneak_shop.enums.Permission;
import sneak_shop.enums.Role;
import sneak_shop.entity.UserEntity;

import java.util.Set;
import java.util.stream.Collectors;

public record AuthResponse(
		String id,
		String fullName,
		String phoneNumber,
		Set<Role> roles,
		Set<Permission> permissions
) {

	public static AuthResponse from(UserEntity user) {
		Set<Permission> permissions = user.getRoles().stream()
				.flatMap(role -> role.getPermissions().stream())
				.collect(Collectors.toSet());

		return new AuthResponse(user.getId(), user.getFullName(), user.getPhoneNumber(), user.getRoles(), permissions);
	}
}
