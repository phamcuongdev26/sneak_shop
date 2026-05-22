package sneak_shop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sneak_shop.entity.UserEntity;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class JwtService {

	private final SecretKey secretKey;
	private final long expirationMinutes;

	public JwtService(
			@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-minutes}") long expirationMinutes
	) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMinutes = expirationMinutes;
	}

	public String generateAccessToken(UserEntity user) {
		Instant now = Instant.now();

		return Jwts.builder()
				.subject(user.getId())
				.claim("phoneNumber", user.getPhoneNumber())
				.claim("roles", user.getRoles())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
				.signWith(secretKey)
				.compact();
	}

	public String getUserId(String token) {
		return getClaims(token).getSubject();
	}

	@SuppressWarnings("unchecked")
	public Set<String> getRoles(String token) {
		List<String> roles = getClaims(token).get("roles", List.class);
		return Set.copyOf(roles);
	}

	private Claims getClaims(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
