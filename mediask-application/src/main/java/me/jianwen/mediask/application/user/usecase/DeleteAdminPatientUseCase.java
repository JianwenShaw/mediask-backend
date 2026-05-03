package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.user.command.DeleteAdminPatientCommand;
import me.jianwen.mediask.domain.user.port.AdminPatientQueryRepository;
import me.jianwen.mediask.domain.user.port.AdminPatientWriteRepository;
import org.springframework.transaction.annotation.Transactional;

public class DeleteAdminPatientUseCase {

    private final AdminPatientQueryRepository adminPatientQueryRepository;
    private final AdminPatientWriteRepository adminPatientWriteRepository;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public DeleteAdminPatientUseCase(
            AdminPatientQueryRepository adminPatientQueryRepository,
            AdminPatientWriteRepository adminPatientWriteRepository,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.adminPatientQueryRepository = adminPatientQueryRepository;
        this.adminPatientWriteRepository = adminPatientWriteRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public void handle(DeleteAdminPatientCommand command, AuditContext auditContext) {
        var detail = adminPatientQueryRepository.findDetailByPatientId(command.patientId())
                .orElseThrow(() -> new me.jianwen.mediask.common.exception.BizException(
                        me.jianwen.mediask.domain.user.exception.UserErrorCode.ADMIN_PATIENT_NOT_FOUND));
        adminPatientWriteRepository.softDelete(command.patientId());
        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.ADMIN_PATIENT_DELETE,
                AuditResourceTypes.PATIENT_PROFILE,
                String.valueOf(command.patientId()),
                detail.userId(),
                null,
                null);
    }
}
