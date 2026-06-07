package sneak_shop.service;

import sneak_shop.dto.request.AddressRequest;
import sneak_shop.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {
    List<AddressResponse> getAll(Integer userId);
    AddressResponse create(Integer userId, AddressRequest req);
    AddressResponse update(Integer userId, Integer addressId, AddressRequest req);
    void delete(Integer userId, Integer addressId);
    AddressResponse setDefault(Integer userId, Integer addressId);
}
