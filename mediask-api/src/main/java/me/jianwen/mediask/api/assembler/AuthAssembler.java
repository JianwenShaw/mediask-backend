package me.jianwen.mediask.api.assembler;

import java.util.List;
import me.jianwen.mediask.api.dto.AdminPatientDetailResponse;
import me.jianwen.mediask.api.dto.AdminPatientListItemResponse;
import me.jianwen.mediask.api.dto.DoctorProfileResponse;
import me.jianwen.mediask.api.dto.CurrentUserResponse;
import me.jianwen.mediask.api.dto.DataScopeRuleResponse;
import me.jianwen.mediask.api.dto.LoginResponse;
import me.jianwen.mediask.api.dto.PatientProfileResponse;
import me.jianwen.mediask.api.dto.RefreshTokenResponse;
import me.jianwen.mediask.application.user.usecase.AuthenticationResult;
import me.jianwen.mediask.api.dto.AdminDepartmentDetailResponse;
import me.jianwen.mediask.api.dto.AdminDepartmentListItemResponse;
import me.jianwen.mediask.api.dto.AdminDoctorDetailResponse;
import me.jianwen.mediask.api.dto.AdminDoctorListItemResponse;
import me.jianwen.mediask.api.dto.DoctorDepartmentAssignmentResponse;
import me.jianwen.mediask.domain.user.model.AdminDepartmentDetail;
import me.jianwen.mediask.domain.user.model.AdminDepartmentListItem;
import me.jianwen.mediask.domain.user.model.AdminDoctorDetail;
import me.jianwen.mediask.domain.user.model.AdminDoctorListItem;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.model.AdminPatientListItem;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.DataScopeRule;
import me.jianwen.mediask.domain.user.model.DoctorProfile;
import me.jianwen.mediask.domain.user.model.PatientProfileSnapshot;
import me.jianwen.mediask.domain.user.model.RoleCode;

public final class AuthAssembler {

    private AuthAssembler() {
    }

    public static LoginResponse toLoginResponse(AuthenticationResult authenticationResult) {
        return new LoginResponse(
                authenticationResult.tokens().accessToken().value(),
                authenticationResult.tokens().accessToken().expiresAt().toEpochMilli(),
                authenticationResult.tokens().refreshTokenSession().tokenValue(),
                authenticationResult.tokens().refreshTokenSession().expiresAt().toEpochMilli(),
                toCurrentUserResponse(authenticationResult.authenticatedUser()));
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

    public static AdminPatientListItemResponse toAdminPatientListItemResponse(AdminPatientListItem adminPatient) {
        return new AdminPatientListItemResponse(
                adminPatient.patientId(),
                adminPatient.userId(),
                adminPatient.patientNo(),
                adminPatient.username(),
                adminPatient.displayName(),
                adminPatient.mobileMasked(),
                adminPatient.gender(),
                adminPatient.birthDate(),
                adminPatient.bloodType(),
                adminPatient.accountStatus());
    }

    public static AdminPatientDetailResponse toAdminPatientDetailResponse(AdminPatientDetail adminPatient) {
        return new AdminPatientDetailResponse(
                adminPatient.patientId(),
                adminPatient.userId(),
                adminPatient.patientNo(),
                adminPatient.username(),
                adminPatient.displayName(),
                adminPatient.mobileMasked(),
                adminPatient.gender(),
                adminPatient.birthDate(),
                adminPatient.bloodType(),
                adminPatient.allergySummary(),
                adminPatient.accountStatus());
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

    public static AdminDoctorListItemResponse toAdminDoctorListItemResponse(AdminDoctorListItem item) {
        return new AdminDoctorListItemResponse(
                String.valueOf(item.doctorId()),
                String.valueOf(item.userId()),
                item.username(),
                item.displayName(),
                item.doctorCode(),
                item.professionalTitle(),
                item.primaryDepartmentName(),
                item.accountStatus());
    }

    public static AdminDoctorDetailResponse toAdminDoctorDetailResponse(AdminDoctorDetail detail) {
        return new AdminDoctorDetailResponse(
                String.valueOf(detail.doctorId()),
                String.valueOf(detail.userId()),
                detail.username(),
                detail.displayName(),
                detail.phone(),
                String.valueOf(detail.hospitalId()),
                detail.doctorCode(),
                detail.professionalTitle(),
                detail.introductionMasked(),
                detail.departments().stream()
                        .map(d -> new DoctorDepartmentAssignmentResponse(
                                String.valueOf(d.departmentId()), d.departmentName(), d.primary()))
                        .toList(),
                detail.accountStatus());
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

    public static AdminDepartmentListItemResponse toAdminDepartmentListItemResponse(AdminDepartmentListItem item) {
        return new AdminDepartmentListItemResponse(
                String.valueOf(item.id()),
                String.valueOf(item.hospitalId()),
                item.deptCode(),
                item.name(),
                item.deptType(),
                item.sortOrder(),
                item.status());
    }

    public static AdminDepartmentDetailResponse toAdminDepartmentDetailResponse(AdminDepartmentDetail detail) {
        return new AdminDepartmentDetailResponse(
                String.valueOf(detail.id()),
                String.valueOf(detail.hospitalId()),
                detail.deptCode(),
                detail.name(),
                detail.deptType(),
                detail.sortOrder(),
                detail.status());
    }

    private static DataScopeRuleResponse toDataScopeRuleResponse(DataScopeRule dataScopeRule) {
        return new DataScopeRuleResponse(
                dataScopeRule.resourceType(),
                dataScopeRule.scopeType().name(),
                dataScopeRule.scopeDepartmentId());
    }
}
