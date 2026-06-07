package sneak_shop.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(AppException.class)
	public ResponseEntity<ErrorResponse> handleAppException(AppException exception, HttpServletRequest request) {
		ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity
				.status(errorCode.getStatus())
				.body(ErrorResponse.of(errorCode, exception.getMessage(), request.getRequestURI(), null));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(
			MethodArgumentNotValidException exception,
			HttpServletRequest request
	) {
		Map<String, String> details = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> details.put(error.getField(), error.getDefaultMessage()));

		ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
		return ResponseEntity
				.status(errorCode.getStatus())
				.body(ErrorResponse.of(errorCode, errorCode.getMessage(), request.getRequestURI(), details));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(
			ConstraintViolationException exception,
			HttpServletRequest request
	) {
		Map<String, String> details = new LinkedHashMap<>();
		exception.getConstraintViolations()
				.forEach(error -> details.put(error.getPropertyPath().toString(), error.getMessage()));

		ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
		return ResponseEntity
				.status(errorCode.getStatus())
				.body(ErrorResponse.of(errorCode, errorCode.getMessage(), request.getRequestURI(), details));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
		ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
		return ResponseEntity
				.status(errorCode.getStatus())
				.body(ErrorResponse.of(errorCode, errorCode.getMessage(), request.getRequestURI(), null));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
		log.error("Unexpected error on {}: {}", request.getRequestURI(), exception.getMessage(), exception);
		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
		Map<String, String> details = new LinkedHashMap<>();
		details.put("exception", exception.getClass().getSimpleName());
		details.put("message", exception.getMessage());
		Throwable cause = exception.getCause();
		if (cause != null) details.put("cause", cause.getMessage());
		return ResponseEntity
				.status(errorCode.getStatus())
				.body(ErrorResponse.of(errorCode, errorCode.getMessage(), request.getRequestURI(), details));
	}
}
