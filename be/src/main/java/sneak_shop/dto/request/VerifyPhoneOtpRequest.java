package sneak_shop.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyPhoneOtpRequest(
        @NotBlank String phone,
        @NotBlank String otp
) {}
