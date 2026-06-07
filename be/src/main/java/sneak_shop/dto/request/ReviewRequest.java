package sneak_shop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReviewRequest(
        @NotNull Integer orderItemId,
        @NotNull @Min(1) @Max(5) Integer rating,
        String comment,
        List<Integer> productImageIds
) {}
