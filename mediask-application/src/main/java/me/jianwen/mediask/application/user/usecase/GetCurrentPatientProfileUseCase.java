package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.user.query.GetCurrentUserQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.PatientProfileSnapshot;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;
import me.jianwen.mediask.domain.user.port.PatientProfileRepository;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.springframework.transaction.annotation.Transactional;

public class GetCurrentPatientProfileUseCase {

    private final UserAuthenticationRepository userAuthenticationRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public GetCurrentPatientProfileUseCase(
            UserAuthenticationRepository userAuthenticationRepository,
            PatientProfileRepository patientProfileRepository,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.userAuthenticationRepository = userAuthenticationRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public PatientProfileSnapshot handle(GetCurrentUserQuery query, AuditContext auditContext) {
        AuthenticatedUser authenticatedUser = userAuthenticationRepository.findAuthenticatedUserById(query.userId())
                .orElseThrow(() -> new BizException(UserErrorCode.AUTHENTICATED_USER_NOT_FOUND));
        if (!authenticatedUser.hasRole(RoleCode.PATIENT)) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
        PatientProfileSnapshot profile = patientProfileRepository.findByUserId(authenticatedUser.userId())
                .orElseThrow(() -> new BizException(UserErrorCode.PATIENT_PROFILE_NOT_FOUND));
        auditTrailService.recordAllowedDataAccess(
                auditContext,
                AuditResourceTypes.PATIENT_PROFILE,
                String.valueOf(profile.patientId()),
                authenticatedUser.userId(),
                null,
                DataAccessPurposeCode.SELF_SERVICE);
        return profile;
    }
}
