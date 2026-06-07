package sneak_shop.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.dto.request.AddressRequest;
import sneak_shop.dto.response.AddressResponse;
import sneak_shop.security.UserContext;
import sneak_shop.service.impl.AddressServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressServiceImpl addressService;

    public AddressController(AddressServiceImpl addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> getAll(@AuthenticationPrincipal UserContext ctx) {
        return ApiResponse.ok(addressService.getAll(ctx.id()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AddressResponse> create(@AuthenticationPrincipal UserContext ctx,
                                               @Valid @RequestBody AddressRequest req) {
        return ApiResponse.ok("Them dia chi thanh cong", addressService.create(ctx.id(), req));
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressResponse> update(@AuthenticationPrincipal UserContext ctx,
                                               @PathVariable Integer id,
                                               @Valid @RequestBody AddressRequest req) {
        return ApiResponse.ok(addressService.update(ctx.id(), id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserContext ctx, @PathVariable Integer id) {
        addressService.delete(ctx.id(), id);
        return ApiResponse.ok("Xoa dia chi thanh cong");
    }

    @PatchMapping("/{id}/default")
    public ApiResponse<AddressResponse> setDefault(@AuthenticationPrincipal UserContext ctx, @PathVariable Integer id) {
        return ApiResponse.ok(addressService.setDefault(ctx.id(), id));
    }
}
