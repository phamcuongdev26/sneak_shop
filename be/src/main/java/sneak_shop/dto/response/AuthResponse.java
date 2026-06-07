package sneak_shop.dto.response;

import sneak_shop.entity.UserEntity;
import sneak_shop.enums.UserGender;

import java.time.Instant;
import java.time.LocalDate;

public record AuthResponse(
        Integer id,
        String email,
        String username,
        String fullName,
        String phone,
        String avatarUrl,
        Boolean emailVerified,
        String role,
        String gender,
        LocalDate birthDate,
        Instant createdAt,
        String tokenType,
        String accessToken
) {
    public static AuthResponse from(UserEntity user, String token) {
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getEmailVerified(),
                user.getRole().name(),
                user.getGender() != null ? user.getGender().name() : null,
                user.getBirthDate(),
                user.getCreatedAt(),
                token != null ? "Bearer" : null,
                token
        );
    }

    public static AuthResponse from(UserEntity user) {
        return from(user, null);
    }
}
