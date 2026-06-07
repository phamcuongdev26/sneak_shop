package sneak_shop.dto.response;

import sneak_shop.entity.AddressEntity;

import java.time.Instant;

public record AddressResponse(
        Integer id,
        Integer userId,
        String recipientName,
        String recipientPhone,
        String address,
        Integer provinceCode,
        Integer districtCode,
        String ward,
        String district,
        String city,
        Boolean isDefault,
        Instant createdAt,
        Instant updatedAt
) {
    public static AddressResponse from(AddressEntity e) {
        return new AddressResponse(
                e.getId(),
                e.getUser().getId(),
                e.getRecipientName(),
                e.getRecipientPhone(),
                e.getAddress(),
                e.getProvinceCode(),
                e.getDistrictCode(),
                e.getWard(),
                e.getDistrict(),
                e.getCity(),
                e.getIsDefault(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
