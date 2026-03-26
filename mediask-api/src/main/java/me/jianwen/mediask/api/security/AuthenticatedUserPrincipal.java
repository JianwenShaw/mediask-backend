package me.jianwen.mediask.api.security;

import java.security.Principal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import me.jianwen.mediask.application.authz.AuthzSubject;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.DataScopeRule;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedUserPrincipal(
        Long userId,
        String username,
        String displayName,
        String userType,
        List<String> roles,
        List<String> permissions,
        List<DataScopeRule> dataScopeRules,
        Long patientId,
        Long doctorId,
        Long primaryDepartmentId,
        Collection<? extends GrantedAuthority> authorities)
        implements Principal {

    public static AuthenticatedUserPrincipal from(AuthenticatedUser authenticatedUser) {
        List<String> roles = authenticatedUser.roles().stream().map(Enum::name).toList();
        List<String> permissions = authenticatedUser.permissions().stream().toList();
        List<DataScopeRule> dataScopeRules = authenticatedUser.dataScopeRules().stream().toList();
        List<GrantedAuthority> roleAuthorities = roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        List<GrantedAuthority> permissionAuthorities = permissions.stream()
                .map(permission -> (GrantedAuthority) new SimpleGrantedAuthority(permission))
                .toList();
        java.util.ArrayList<GrantedAuthority> authorities = new java.util.ArrayList<>(roleAuthorities.size()
                + permissionAuthorities.size());
        authorities.addAll(roleAuthorities);
        authorities.addAll(permissionAuthorities);
        return new AuthenticatedUserPrincipal(
                authenticatedUser.userId(),
                authenticatedUser.username(),
                authenticatedUser.displayName(),
                authenticatedUser.userType().name(),
                roles,
                permissions,
                dataScopeRules,
                authenticatedUser.patientId(),
                authenticatedUser.doctorId(),
                authenticatedUser.primaryDepartmentId(),
                authorities);
    }

    @Override
    public String getName() {
        return username;
    }

    public AuthzSubject toAuthzSubject() {
        return new AuthzSubject(
                userId,
                new LinkedHashSet<>(permissions),
                new LinkedHashSet<>(dataScopeRules),
                primaryDepartmentId);
    }
}
