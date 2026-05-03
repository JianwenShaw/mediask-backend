package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.user.command.UpdateCurrentPatientProfileCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.PatientProfileDraft;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.port.PatientProfileWriteRepository;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.springframework.transaction.annotation.Transactional;

public class UpdateCurrentPatientProfileUseCase {

    private final UserAuthenticationRepository userAuthenticationRepository;
    private final PatientProfileWriteRepository patientProfileWriteRepository;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public UpdateCurrentPatientProfileUseCase(
            UserAuthenticationRepository userAuthenticationRepository,
            PatientProfileWriteRepository patientProfileWriteRepository,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.userAuthenticationRepository = userAuthenticationRepository;
        this.patientProfileWriteRepository = patientProfileWriteRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public void handle(UpdateCurrentPatientProfileCommand command, AuditContext auditContext) {
        AuthenticatedUser authenticatedUser = userAuthenticationRepository.findAuthenticatedUserById(command.userId())
                .orElseThrow(() -> new BizException(UserErrorCode.AUTHENTICATED_USER_NOT_FOUND));
        if (!authenticatedUser.hasRole(RoleCode.PATIENT)) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
        patientProfileWriteRepository.updateByUserId(
                authenticatedUser.userId(),
                new PatientProfileDraft(
                        command.gender(),
                        command.birthDate(),
                        command.bloodType(),
                        command.allergySummary()));
        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.PATIENT_PROFILE_UPDATE,
                AuditResourceTypes.PATIENT_PROFILE,
                String.valueOf(authenticatedUser.patientId()),
                authenticatedUser.userId(),
                null,
                null);
    }
}
