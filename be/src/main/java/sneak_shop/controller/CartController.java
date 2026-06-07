package sneak_shop.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.dto.request.CartRequest;
import sneak_shop.dto.response.CartItemResponse;
import sneak_shop.security.UserContext;
import sneak_shop.service.impl.CartServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartServiceImpl cartService;

    public CartController(CartServiceImpl cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<List<CartItemResponse>> getCart(@AuthenticationPrincipal UserContext ctx) {
        return ApiResponse.ok(cartService.getCart(ctx.id()));
    }

    @PostMapping
    public ApiResponse<CartItemResponse> addItem(@AuthenticationPrincipal UserContext ctx,
                                                  @Valid @RequestBody CartRequest req) {
        return ApiResponse.ok(cartService.addOrUpdate(ctx.id(), req));
    }

    @PutMapping("/{itemId}")
    public ApiResponse<CartItemResponse> updateQuantity(@AuthenticationPrincipal UserContext ctx,
                                                         @PathVariable Integer itemId,
                                                         @RequestParam Integer quantity) {
        return ApiResponse.ok(cartService.updateQuantity(ctx.id(), itemId, quantity));
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> removeItem(@AuthenticationPrincipal UserContext ctx, @PathVariable Integer itemId) {
        cartService.removeItem(ctx.id(), itemId);
        return ApiResponse.ok("Xoa khoi gio hang thanh cong");
    }

    @DeleteMapping
    public ApiResponse<Void> clearCart(@AuthenticationPrincipal UserContext ctx) {
        cartService.clearCart(ctx.id());
        return ApiResponse.ok("Da xoa gio hang");
    }
}
