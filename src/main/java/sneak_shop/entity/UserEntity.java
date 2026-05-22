package sneak_shop.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(nullable = false, length = 100)
	private String fullName;

	@Column(nullable = false, unique = true, length = 20)
	private String phoneNumber;

	@Column(nullable = false)
	private String password;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private Set<UserRoleEntity> userRoles = new HashSet<>();

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();


	public UserEntity(String fullName, String phoneNumber, String password, Set<RoleEntity> roles) {
		this.fullName = fullName;
		this.phoneNumber = phoneNumber;
		this.password = password;
		roles.forEach(this::addRole);
	}

	public Set<SimpleGrantedAuthority> getAuthorities() {
		Set<SimpleGrantedAuthority> authorities = getPermissions().stream()
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toSet());

		getRoles().stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
				.forEach(authorities::add);

		return authorities;
	}

	public Set<String> getRoles() {
		return userRoles.stream()
				.map(UserRoleEntity::getRole)
				.map(RoleEntity::getName)
				.collect(Collectors.toSet());
	}

	public Set<String> getPermissions() {
		return userRoles.stream()
				.map(UserRoleEntity::getRole)
				.flatMap(role -> role.getPermissions().stream())
				.collect(Collectors.toSet());
	}

	public void addRole(RoleEntity role) {
		boolean exists = userRoles.stream()
				.anyMatch(userRole -> userRole.getRole().getName().equals(role.getName()));

		if (!exists) {
			userRoles.add(UserRoleEntity.builder()
					.user(this)
					.role(role)
					.build());
		}
	}
}
