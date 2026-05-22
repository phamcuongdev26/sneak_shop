package sneak_shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "Ho ten khong duoc de trong")
		@Size(min = 2, max = 100, message = "Ho ten phai tu 2 den 100 ky tu")
		String fullName,

		@NotBlank(message = "So dien thoai khong duoc de trong")
		@Pattern(regexp = "^(0|\\+84)(3|5|7|8|9)\\d{8}$", message = "So dien thoai khong dung dinh dang")
		String phoneNumber,

		@NotBlank(message = "Password khong duoc de trong")
		@Size(min = 6, max = 100, message = "Password phai tu 6 den 100 ky tu")
		String password
) {
}
