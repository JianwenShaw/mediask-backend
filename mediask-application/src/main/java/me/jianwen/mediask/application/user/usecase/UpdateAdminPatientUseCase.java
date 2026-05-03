package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.user.command.UpdateAdminPatientCommand;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.model.AdminPatientUpdateDraft;
import me.jianwen.mediask.domain.user.port.AdminPatientWriteRepository;
import org.springframework.transaction.annotation.Transactional;

public class UpdateAdminPatientUseCase {

    private final AdminPatientWriteRepository adminPatientWriteRepository;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public UpdateAdminPatientUseCase(
            AdminPatientWriteRepository adminPatientWriteRepository,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.adminPatientWriteRepository = adminPatientWriteRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public AdminPatientDetail handle(UpdateAdminPatientCommand command, AuditContext auditContext) {
        AdminPatientDetail detail = adminPatientWriteRepository.update(
                command.patientId(),
                new AdminPatientUpdateDraft(
                        command.displayName(),
                        command.mobileMasked(),
                        command.gender(),
                        command.birthDate(),
                        command.bloodType(),
                        command.allergySummary()));
        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.ADMIN_PATIENT_UPDATE,
                AuditResourceTypes.PATIENT_PROFILE,
                String.valueOf(detail.patientId()),
                detail.userId(),
                null,
                null);
        return detail;
    }
}
