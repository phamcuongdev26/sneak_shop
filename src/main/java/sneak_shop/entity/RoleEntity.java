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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class RoleEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(nullable = false, unique = true, length = 50)
	private String name;

	@OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private Set<RolePermissionEntity> rolePermissions = new HashSet<>();

	public RoleEntity(String name) {
		this.name = name;
	}

	public Set<String> getPermissions() {
		return rolePermissions.stream()
				.map(rolePermission -> rolePermission.getPermission().getName())
				.collect(Collectors.toSet());
	}

	public void addPermission(PermissionEntity permission) {
		boolean exists = rolePermissions.stream()
				.anyMatch(rolePermission -> rolePermission.getPermission().getName().equals(permission.getName()));

		if (!exists) {
			rolePermissions.add(RolePermissionEntity.builder()
					.role(this)
					.permission(permission)
					.build());
		}
	}
}
