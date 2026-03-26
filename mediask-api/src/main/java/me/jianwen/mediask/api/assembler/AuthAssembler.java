package me.jianwen.mediask.api.assembler;

import java.util.List;
import me.jianwen.mediask.api.dto.DoctorProfileResponse;
import me.jianwen.mediask.api.dto.CurrentUserResponse;
import me.jianwen.mediask.api.dto.DataScopeRuleResponse;
import me.jianwen.mediask.api.dto.LoginResponse;
import me.jianwen.mediask.api.dto.PatientProfileResponse;
import me.jianwen.mediask.api.dto.RefreshTokenResponse;
import me.jianwen.mediask.application.user.usecase.AuthenticationResult;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.DataScopeRule;
import me.jianwen.mediask.domain.user.model.DoctorProfile;
import me.jianwen.mediask.domain.user.model.PatientProfileSnapshot;
import me.jianwen.mediask.domain.user.model.RoleCode;

public final class AuthAssembler {

    private AuthAssembler() {
    }

    public static LoginResponse toLoginResponse(AuthenticationResult authenticationResult) {
        CurrentUserResponse userContext = toCurrentUserResponse(authenticationResult.authenticatedUser());
        return new LoginResponse(
                authenticationResult.tokens().accessToken().value(),
                authenticationResult.tokens().accessToken().expiresAt().toEpochMilli(),
                authenticationResult.tokens().refreshTokenSession().tokenValue(),
                authenticationResult.tokens().refreshTokenSession().expiresAt().toEpochMilli(),
                userContext);
    }

    public static RefreshTokenResponse toRefreshTokenResponse(AuthenticationResult authenticationResult) {
        CurrentUserResponse userContext = toCurrentUserResponse(authenticationResult.authenticatedUser());
        return new RefreshTokenResponse(
                authenticationResult.tokens().accessToken().value(),
                authenticationResult.tokens().accessToken().expiresAt().toEpochMilli(),
                authenticationResult.tokens().refreshTokenSession().tokenValue(),
                authenticationResult.tokens().refreshTokenSession().expiresAt().toEpochMilli(),
                userContext);
    }

    public static CurrentUserResponse toCurrentUserResponse(AuthenticatedUser authenticatedUser) {
        return new CurrentUserResponse(
                authenticatedUser.userId(),
                authenticatedUser.username(),
                authenticatedUser.displayName(),
                authenticatedUser.userType().name(),
                toRoleCodes(authenticatedUser),
                toPermissionCodes(authenticatedUser),
                toDataScopeRules(authenticatedUser),
                authenticatedUser.patientId(),
                authenticatedUser.doctorId(),
                authenticatedUser.primaryDepartmentId());
    }

    public static PatientProfileResponse toPatientProfileResponse(PatientProfileSnapshot patientProfile) {
        return new PatientProfileResponse(
                patientProfile.patientId(),
                patientProfile.patientNo(),
                patientProfile.gender(),
                patientProfile.birthDate(),
                patientProfile.bloodType(),
                patientProfile.allergySummary());
    }

    public static DoctorProfileResponse toDoctorProfileResponse(DoctorProfile doctorProfile) {
        return new DoctorProfileResponse(
                doctorProfile.doctorId(),
                doctorProfile.doctorCode(),
                doctorProfile.professionalTitle(),
                doctorProfile.introductionMasked(),
                doctorProfile.hospitalId(),
                doctorProfile.primaryDepartmentId(),
                doctorProfile.primaryDepartmentName());
    }

    private static List<String> toRoleCodes(AuthenticatedUser authenticatedUser) {
        return authenticatedUser.roles().stream().map(RoleCode::name).toList();
    }

    private static List<String> toPermissionCodes(AuthenticatedUser authenticatedUser) {
        return authenticatedUser.permissions().stream().toList();
    }

    private static List<DataScopeRuleResponse> toDataScopeRules(AuthenticatedUser authenticatedUser) {
        return authenticatedUser.dataScopeRules().stream()
                .map(AuthAssembler::toDataScopeRuleResponse)
                .toList();
    }

    private static DataScopeRuleResponse toDataScopeRuleResponse(DataScopeRule dataScopeRule) {
        return new DataScopeRuleResponse(
                dataScopeRule.resourceType(),
                dataScopeRule.scopeType().name(),
                dataScopeRule.scopeDepartmentId());
    }
}
