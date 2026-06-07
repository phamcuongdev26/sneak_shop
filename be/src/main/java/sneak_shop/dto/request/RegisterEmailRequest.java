package sneak_shop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterEmailRequest(
        @NotBlank @Email
        String email,

        @NotBlank
        @Size(min = 2, max = 255)
        String fullName,

        String phone,

        @NotBlank
        @Size(min = 6)
        String password
) {}
