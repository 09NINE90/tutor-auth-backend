package ru.razumoff.config.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import ru.razumoff.commonlib.exceptions.ErrorCode;
import ru.razumoff.commonlib.exceptions.PlatformException;

import java.util.Collection;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class JwtUserPrincipal {

    private UUID id;
    private String email;
    private String token;
    private Collection<? extends GrantedAuthority> authorities;

    public void requireRole(String role) {
        if (!hasRole(role)) {
            throw new PlatformException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }

    public boolean hasRole(String role) {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
