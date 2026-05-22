package sneak_shop.enums;

import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public enum Role {
	ADMIN(Set.of(
			Permission.ROLE_VIEW,
			Permission.ROLE_CREATE,
			Permission.ROLE_UPDATE,
			Permission.ROLE_DELETE,
			Permission.USER_VIEW,
			Permission.USER_CREATE,
			Permission.USER_UPDATE,
			Permission.USER_DELETE,
			Permission.PRODUCT_VIEW,
			Permission.PRODUCT_CREATE,
			Permission.PRODUCT_UPDATE,
			Permission.PRODUCT_DELETE,
			Permission.ORDER_VIEW,
			Permission.ORDER_UPDATE
	)),
	STAFF(Set.of(
			Permission.PRODUCT_VIEW,
			Permission.PRODUCT_CREATE,
			Permission.PRODUCT_UPDATE,
			Permission.ORDER_VIEW,
			Permission.ORDER_UPDATE
	)),
	CUSTOMER(Set.of(
			Permission.PRODUCT_VIEW,
			Permission.ORDER_CREATE,
			Permission.ORDER_VIEW_OWN
	));

	private final Set<Permission> permissions;

	Role(Set<Permission> permissions) {
		this.permissions = permissions;
	}

	public Set<SimpleGrantedAuthority> getAuthorities() {
		Set<SimpleGrantedAuthority> authorities = permissions.stream()
				.map(permission -> new SimpleGrantedAuthority(permission.name()))
				.collect(Collectors.toSet());

		authorities.add(new SimpleGrantedAuthority("ROLE_" + name()));
		return authorities;
	}

	public static Set<SimpleGrantedAuthority> combineAuthorities(Role... roles) {
		return Stream.of(roles)
				.flatMap(role -> role.getAuthorities().stream())
				.collect(Collectors.toSet());
	}
}
