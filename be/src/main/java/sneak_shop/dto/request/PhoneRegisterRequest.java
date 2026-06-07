package sneak_shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PhoneRegisterRequest(
        @NotBlank
        String phone,

        @NotBlank
        @Size(min = 2, max = 255)
        String fullName,

        @NotBlank
        @Size(min = 6)
        String password
) {}
