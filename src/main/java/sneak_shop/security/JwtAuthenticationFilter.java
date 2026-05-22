package sneak_shop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.exception.ErrorResponse;
import sneak_shop.entity.UserEntity;
import sneak_shop.repository.UserRepository;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

	public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository, ObjectMapper objectMapper) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String token = resolveToken(request);
		if (token != null && !authenticate(request, response, token)) {
			return;
		}

		try {
			filterChain.doFilter(request, response);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	private boolean authenticate(HttpServletRequest request, HttpServletResponse response, String token) throws IOException {
		try {
			String userId = jwtService.getUserId(token);
			UserEntity user = userRepository.findById(userId)
					.filter(UserEntity::isEnabled)
					.orElseThrow(() -> new JwtException("User khong ton tai hoac da bi khoa"));
			UserContext principal = UserContext.from(user);

			SecurityContextHolder.getContext().setAuthentication(
					new UsernamePasswordAuthenticationToken(
							principal,
							null,
							principal.authorities()
					)
			);
			return true;
		} catch (JwtException | IllegalArgumentException exception) {
			writeUnauthorizedResponse(request, response);
			return false;
		}
	}

	private String resolveToken(HttpServletRequest request) {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			return null;
		}

		return authorization.substring(BEARER_PREFIX.length());
	}

	private void writeUnauthorizedResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(
				response.getOutputStream(),
				ErrorResponse.of(errorCode, "Token khong hop le hoac da het han", request.getRequestURI(), null)
		);
	}
}
