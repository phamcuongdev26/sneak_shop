package sneak_shop.service;

import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.GuestClassRequest;
import sneak_shop.dto.response.GuestClassResponse;

import java.util.List;

public interface GuestClassService {
    PageResponse<GuestClassResponse> getAll(int page, int size);
    List<GuestClassResponse> getByUser(Integer userId);
    GuestClassResponse create(GuestClassRequest req);
    GuestClassResponse update(Integer id, GuestClassRequest req);
    void delete(Integer id);
}
