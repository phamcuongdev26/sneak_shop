package sneak_shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 255) String recipientName,
        @NotBlank @Size(max = 20) String recipientPhone,
        @NotBlank @Size(max = 500) String address,
        @NotNull Integer provinceCode,
        @NotNull Integer districtCode,
        String ward,
        String district,
        @NotBlank @Size(max = 100) String city,
        Boolean isDefault
) {}
