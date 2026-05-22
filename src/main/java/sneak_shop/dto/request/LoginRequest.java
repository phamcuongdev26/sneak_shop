package sneak_shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
		@NotBlank(message = "So dien thoai khong duoc de trong")
		@Pattern(regexp = "^(0|\\+84)(3|5|7|8|9)\\d{8}$", message = "So dien thoai khong dung dinh dang")
		String phoneNumber,

		@NotBlank(message = "Password khong duoc de trong")
		String password
) {
}
