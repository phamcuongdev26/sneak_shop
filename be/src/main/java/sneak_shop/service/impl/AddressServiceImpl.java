package sneak_shop.service.impl;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.dto.request.AddressRequest;
import sneak_shop.dto.response.AddressResponse;
import sneak_shop.entity.AddressEntity;
import sneak_shop.entity.UserEntity;
import sneak_shop.repository.AddressRepository;
import sneak_shop.repository.UserRepository;
import sneak_shop.service.AddressService;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressServiceImpl(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    public List<AddressResponse> getAll(Integer userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream().map(AddressResponse::from).toList();
    }

    @Transactional
    public AddressResponse create(Integer userId, AddressRequest req) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User khong ton tai"));

        boolean firstAddress = !addressRepository.existsByUserId(userId);
        boolean makeDefault = Boolean.TRUE.equals(req.isDefault()) || firstAddress;

        if (makeDefault) clearDefault(userId);

        AddressEntity address = AddressEntity.builder()
                .user(user)
                .recipientName(req.recipientName())
                .recipientPhone(req.recipientPhone())
                .address(req.address())
                .provinceCode(req.provinceCode())
                .districtCode(req.districtCode())
                .ward(req.ward())
                .district(req.district())
                .city(req.city())
                .isDefault(makeDefault)
                .build();
        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(Integer userId, Integer addressId, AddressRequest req) {
        AddressEntity address = addressRepository.findById(addressId)
                .filter(a -> a.getUser().getId().equals(userId))
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Dia chi khong ton tai"));

        address.setRecipientName(req.recipientName());
        address.setRecipientPhone(req.recipientPhone());
        address.setAddress(req.address());
        address.setProvinceCode(req.provinceCode());
        address.setDistrictCode(req.districtCode());
        address.setWard(req.ward());
        address.setDistrict(req.district());
        address.setCity(req.city());
        if (Boolean.TRUE.equals(req.isDefault())) {
            clearDefault(userId);
            address.setIsDefault(true);
        } else if (Boolean.FALSE.equals(req.isDefault())) {
            address.setIsDefault(false);
        }
        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public void delete(Integer userId, Integer addressId) {
        AddressEntity address = addressRepository.findById(addressId)
                .filter(a -> a.getUser().getId().equals(userId))
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Dia chi khong ton tai"));
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);
        if (wasDefault) {
            addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                    .stream().findFirst()
                    .ifPresent(next -> { next.setIsDefault(true); addressRepository.save(next); });
        }
    }

    @Transactional
    public AddressResponse setDefault(Integer userId, Integer addressId) {
        clearDefault(userId);
        AddressEntity address = addressRepository.findById(addressId)
                .filter(a -> a.getUser().getId().equals(userId))
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Dia chi khong ton tai"));
        address.setIsDefault(true);
        return AddressResponse.from(addressRepository.save(address));
    }

    private void clearDefault(Integer userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(a -> { a.setIsDefault(false); addressRepository.save(a); });
    }
}
