package sneak_shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
		name = "role_permissions",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_role_permissions_role_id_permission_id",
				columnNames = {"role_id", "permission_id"}
		)
)
public class RolePermissionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "role_id", nullable = false)
	private RoleEntity role;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "permission_id", nullable = false)
	private PermissionEntity permission;
}
