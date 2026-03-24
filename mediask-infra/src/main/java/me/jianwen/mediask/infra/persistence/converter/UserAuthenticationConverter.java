package me.jianwen.mediask.infra.persistence.converter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import me.jianwen.mediask.domain.user.model.AccountStatus;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.LoginAccount;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import me.jianwen.mediask.infra.persistence.dataobject.UserDO;

public final class UserAuthenticationConverter {

    public LoginAccount toLoginAccount(
            UserDO userDO,
            List<String> roleCodes,
            List<String> permissionCodes,
            Long patientId,
            Long doctorId,
            Long primaryDepartmentId) {
        return new LoginAccount(
                toAuthenticatedUser(userDO, roleCodes, permissionCodes, patientId, doctorId, primaryDepartmentId),
                userDO.getPasswordHash(),
                AccountStatus.fromCode(userDO.getAccountStatus()));
    }

    public AuthenticatedUser toAuthenticatedUser(
            UserDO userDO,
            List<String> roleCodes,
            List<String> permissionCodes,
            Long patientId,
            Long doctorId,
            Long primaryDepartmentId) {
        return new AuthenticatedUser(
                userDO.getId(),
                userDO.getUsername(),
                userDO.getDisplayName(),
                UserType.fromCode(userDO.getUserType()),
                toRoleCodes(roleCodes),
                toPermissions(permissionCodes),
                patientId,
                doctorId,
                primaryDepartmentId);
    }

    private Set<RoleCode> toRoleCodes(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<RoleCode> normalized = new LinkedHashSet<>();
        for (String roleCode : roleCodes) {
            normalized.add(RoleCode.fromCode(roleCode));
        }
        return Collections.unmodifiableSet(normalized);
    }

    private Set<String> toPermissions(List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String permissionCode : permissionCodes) {
            if (permissionCode != null && !permissionCode.isBlank()) {
                normalized.add(permissionCode.trim());
            }
        }
        return Collections.unmodifiableSet(normalized);
    }
}
