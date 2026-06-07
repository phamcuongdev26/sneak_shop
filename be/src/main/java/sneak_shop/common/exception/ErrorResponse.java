package sneak_shop.common.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
		Instant timestamp,
		int status,
		String error,
		String code,
		String message,
		String path,
		Map<String, String> details
) {

	public static ErrorResponse of(ErrorCode errorCode, String message, String path, Map<String, String> details) {
		return new ErrorResponse(
				Instant.now(),
				errorCode.getStatus().value(),
				errorCode.getStatus().getReasonPhrase(),
				errorCode.getCode(),
				message,
				path,
				details
		);
	}
}
