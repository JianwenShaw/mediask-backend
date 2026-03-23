package me.jianwen.mediask.api.security;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedUserPrincipal(
        Long userId,
        String username,
        String displayName,
        String userType,
        List<String> roles,
        Long patientId,
        Long doctorId,
        Long primaryDepartmentId,
        Collection<? extends GrantedAuthority> authorities)
        implements Principal {

    public static AuthenticatedUserPrincipal from(AuthenticatedUser authenticatedUser) {
        List<String> roles = authenticatedUser.roles().stream().map(Enum::name).toList();
        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return new AuthenticatedUserPrincipal(
                authenticatedUser.userId(),
                authenticatedUser.username(),
                authenticatedUser.displayName(),
                authenticatedUser.userType().name(),
                roles,
                authenticatedUser.patientId(),
                authenticatedUser.doctorId(),
                authenticatedUser.primaryDepartmentId(),
                authorities);
    }

    @Override
    public String getName() {
        return username;
    }
}
