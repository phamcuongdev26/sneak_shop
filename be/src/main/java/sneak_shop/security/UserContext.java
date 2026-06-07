package sneak_shop.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import sneak_shop.entity.UserEntity;

import java.util.Collection;
import java.util.List;

public record UserContext(
        Integer id,
        String email,
        String fullName,
        String role,
        Collection<? extends GrantedAuthority> authorities
) {
    public static UserContext from(UserEntity user) {
        String roleName = "ROLE_" + user.getRole().name().toUpperCase();
        return new UserContext(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                List.of(new SimpleGrantedAuthority(roleName))
        );
    }

    public boolean isAdmin() { return "admin".equals(role); }
    public boolean isUser()  { return "user".equals(role); }
}
