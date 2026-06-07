package sneak_shop.service;

import sneak_shop.dto.request.CartRequest;
import sneak_shop.dto.response.CartItemResponse;

import java.util.List;

public interface CartService {
    List<CartItemResponse> getCart(Integer userId);
    CartItemResponse addOrUpdate(Integer userId, CartRequest req);
    CartItemResponse updateQuantity(Integer userId, Integer itemId, Integer quantity);
    void removeItem(Integer userId, Integer itemId);
    void clearCart(Integer userId);
}
