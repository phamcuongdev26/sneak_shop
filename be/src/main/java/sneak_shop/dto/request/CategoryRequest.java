package sneak_shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import sneak_shop.enums.CategoryStatus;

public record CategoryRequest(
        @NotBlank String name,
        @NotBlank String slug,
        String description,
        String imageUrl,
        Integer parentId,
        Integer sortOrder,
        CategoryStatus status
) {}
