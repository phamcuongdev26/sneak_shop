package sneak_shop.security;

import org.springframework.security.core.GrantedAuthority;
import sneak_shop.entity.UserEntity;

import java.util.Collection;
import java.util.Set;

public record UserContext(
		String id,
		String fullName,
		String phoneNumber,
		Set<String> roles,
		Set<String> permissions,
		Collection<? extends GrantedAuthority> authorities
) {

	public static UserContext from(UserEntity user) {
		return new UserContext(
				user.getId(),
				user.getFullName(),
				user.getPhoneNumber(),
				user.getRoles(),
				user.getPermissions(),
				user.getAuthorities()
		);
	}
}
