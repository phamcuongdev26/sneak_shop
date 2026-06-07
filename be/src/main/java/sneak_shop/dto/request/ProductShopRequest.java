package sneak_shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductShopRequest(
        @NotBlank @Size(max = 250) String name,
        @Size(max = 500) String avatar
) {}
