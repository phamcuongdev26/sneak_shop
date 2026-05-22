package sneak_shop.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
	INVALID_REQUEST("INVALID_REQUEST", "Request khong hop le", HttpStatus.BAD_REQUEST),
	UNAUTHORIZED("UNAUTHORIZED", "Ban can dang nhap de thuc hien thao tac nay", HttpStatus.UNAUTHORIZED),
	ACCESS_DENIED("ACCESS_DENIED", "Ban khong co quyen thuc hien thao tac nay", HttpStatus.FORBIDDEN),
	RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Khong tim thay tai nguyen", HttpStatus.NOT_FOUND),
	CONFLICT("CONFLICT", "Du lieu da ton tai hoac bi xung dot", HttpStatus.CONFLICT),
	INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "He thong dang gap loi", HttpStatus.INTERNAL_SERVER_ERROR);

	private final String code;
	private final String message;
	private final HttpStatus status;

	ErrorCode(String code, String message, HttpStatus status) {
		this.code = code;
		this.message = message;
		this.status = status;
	}

}
