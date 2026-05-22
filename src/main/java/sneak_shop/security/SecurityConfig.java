package sneak_shop.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import sneak_shop.entity.PermissionEntity;
import sneak_shop.entity.RoleEntity;
import sneak_shop.entity.UserEntity;
import sneak_shop.repository.PermissionRepository;
import sneak_shop.repository.RoleRepository;
import sneak_shop.repository.UserRepository;

import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler,
			JwtAuthenticationFilter jwtAuthenticationFilter
	) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler)
				)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health").permitAll()
						.requestMatchers("/api/auth/**").permitAll()
						.anyRequest().authenticated()
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public UserDetailsService userDetailsService(UserRepository userRepository) {
		return phoneNumber -> userRepository.findByPhoneNumber(phoneNumber)
				.map(user -> org.springframework.security.core.userdetails.User.withUsername(user.getPhoneNumber())
						.password(user.getPassword())
						.authorities(user.getAuthorities())
						.disabled(!user.isEnabled())
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("Khong tim thay user voi so dien thoai: " + phoneNumber));
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CommandLineRunner seedDefaultUsers(
			UserRepository userRepository,
			RoleRepository roleRepository,
			PermissionRepository permissionRepository,
			PasswordEncoder passwordEncoder
	) {
		return args -> {
			seedRolesAndPermissions(roleRepository, permissionRepository);

			RoleEntity adminRole = roleRepository.findByName("ADMIN").orElseThrow();

			createDefaultUser(userRepository, passwordEncoder, "Admin", "0900000001", "admin123", Set.of(adminRole));
		};
	}

	private void seedRolesAndPermissions(RoleRepository roleRepository, PermissionRepository permissionRepository) {
		Map<String, Set<String>> rolePermissions = Map.of(
				"ADMIN", Set.of(
						"ROLE_VIEW",
						"ROLE_CREATE",
						"ROLE_UPDATE",
						"ROLE_DELETE",
						"USER_VIEW",
						"USER_CREATE",
						"USER_UPDATE",
						"USER_DELETE",
						"PRODUCT_VIEW",
						"PRODUCT_CREATE",
						"PRODUCT_UPDATE",
						"PRODUCT_DELETE",
						"ORDER_VIEW",
						"ORDER_UPDATE"
				),
				"STAFF", Set.of(
						"PRODUCT_VIEW",
						"PRODUCT_CREATE",
						"PRODUCT_UPDATE",
						"ORDER_VIEW",
						"ORDER_UPDATE"
				),
				"CUSTOMER", Set.of(
						"PRODUCT_VIEW",
						"ORDER_CREATE",
						"ORDER_VIEW_OWN"
				)
		);

		rolePermissions.forEach((roleName, permissions) -> {
			RoleEntity role = roleRepository.findByName(roleName)
					.orElseGet(() -> roleRepository.save(new RoleEntity(roleName)));

			permissions.stream()
					.map(permissionName -> permissionRepository.findByName(permissionName)
							.orElseGet(() -> permissionRepository.save(new PermissionEntity(permissionName))))
					.forEach(role::addPermission);

			roleRepository.save(role);
		});
	}

	private void createDefaultUser(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			String fullName,
			String phoneNumber,
			String rawPassword,
			Set<RoleEntity> roles
	) {
		if (userRepository.existsByPhoneNumber(phoneNumber)) {
			return;
		}

		userRepository.save(new UserEntity(fullName, phoneNumber, passwordEncoder.encode(rawPassword), roles));
	}
}
