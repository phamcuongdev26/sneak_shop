package sneak_shop.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
import sneak_shop.enums.Permission;
import sneak_shop.enums.Role;
import sneak_shop.entity.UserEntity;
import sneak_shop.repository.UserRepository;

import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler
	) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
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
				.httpBasic(Customizer.withDefaults());

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
	public CommandLineRunner seedDefaultUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			createDefaultUser(userRepository, passwordEncoder, "Admin", "0900000001", "admin123", Set.of(Role.ADMIN));
			createDefaultUser(userRepository, passwordEncoder, "Staff", "0900000002", "staff123", Set.of(Role.STAFF));
			createDefaultUser(userRepository, passwordEncoder, "Customer", "0900000003", "customer123", Set.of(Role.CUSTOMER));
		};
	}

	private void createDefaultUser(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			String fullName,
			String phoneNumber,
			String rawPassword,
			Set<Role> roles
	) {
		if (userRepository.existsByPhoneNumber(phoneNumber)) {
			return;
		}

		userRepository.save(new UserEntity(fullName, phoneNumber, passwordEncoder.encode(rawPassword), roles));
	}
}
