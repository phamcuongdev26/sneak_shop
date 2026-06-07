package sneak_shop.controller.admin;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.GuestClassRequest;
import sneak_shop.dto.response.GuestClassResponse;
import sneak_shop.service.impl.GuestClassServiceImpl;

@RestController
@RequestMapping("/api/admin/guest-class")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminGuestClassController {

    private final GuestClassServiceImpl guestClassService;

    public AdminGuestClassController(GuestClassServiceImpl guestClassService) {
        this.guestClassService = guestClassService;
    }

    @GetMapping
    public ApiResponse<PageResponse<GuestClassResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(guestClassService.getAll(page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GuestClassResponse> create(@Valid @RequestBody GuestClassRequest req) {
        return ApiResponse.ok("Tao thanh cong", guestClassService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<GuestClassResponse> update(@PathVariable Integer id,
                                                   @Valid @RequestBody GuestClassRequest req) {
        return ApiResponse.ok("Cap nhat thanh cong", guestClassService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        guestClassService.delete(id);
    }
}
