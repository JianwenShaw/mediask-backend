package me.jianwen.mediask.api.assembler;

import java.util.List;
import me.jianwen.mediask.api.dto.CurrentUserResponse;
import me.jianwen.mediask.api.dto.LoginResponse;
import me.jianwen.mediask.application.user.usecase.LoginResult;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.RoleCode;

public final class AuthAssembler {

    private AuthAssembler() {
    }

    public static LoginResponse toLoginResponse(LoginResult loginResult) {
        AuthenticatedUser authenticatedUser = loginResult.authenticatedUser();
        return new LoginResponse(
                loginResult.accessToken(),
                authenticatedUser.userId(),
                authenticatedUser.username(),
                authenticatedUser.displayName(),
                authenticatedUser.userType().name(),
                toRoleCodes(authenticatedUser),
                authenticatedUser.patientId(),
                authenticatedUser.doctorId(),
                authenticatedUser.primaryDepartmentId());
    }

    public static CurrentUserResponse toCurrentUserResponse(AuthenticatedUser authenticatedUser) {
        return new CurrentUserResponse(
                authenticatedUser.userId(),
                authenticatedUser.username(),
                authenticatedUser.displayName(),
                authenticatedUser.userType().name(),
                toRoleCodes(authenticatedUser),
                authenticatedUser.patientId(),
                authenticatedUser.doctorId(),
                authenticatedUser.primaryDepartmentId());
    }

    private static List<String> toRoleCodes(AuthenticatedUser authenticatedUser) {
        return authenticatedUser.roles().stream().map(RoleCode::name).toList();
    }
}
