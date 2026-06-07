package sneak_shop.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ShopReplyRequest(@NotBlank String reply) {}
