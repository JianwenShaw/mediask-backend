package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.user.command.DeleteAdminDoctorCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.port.AdminDoctorQueryRepository;
import me.jianwen.mediask.domain.user.port.AdminDoctorWriteRepository;
import org.springframework.transaction.annotation.Transactional;

public class DeleteAdminDoctorUseCase {

    private final AdminDoctorQueryRepository adminDoctorQueryRepository;
    private final AdminDoctorWriteRepository adminDoctorWriteRepository;
    private final AuditTrailService auditTrailService;

    public DeleteAdminDoctorUseCase(
            AdminDoctorQueryRepository adminDoctorQueryRepository,
            AdminDoctorWriteRepository adminDoctorWriteRepository,
            AuditTrailService auditTrailService) {
        this.adminDoctorQueryRepository = adminDoctorQueryRepository;
        this.adminDoctorWriteRepository = adminDoctorWriteRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public void handle(DeleteAdminDoctorCommand command, AuditContext auditContext) {
        var detail = adminDoctorQueryRepository.findDetailByDoctorId(command.doctorId())
                .orElseThrow(() -> new BizException(UserErrorCode.ADMIN_DOCTOR_NOT_FOUND));
        adminDoctorWriteRepository.softDelete(command.doctorId());
        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.ADMIN_DOCTOR_DELETE,
                AuditResourceTypes.DOCTOR_PROFILE,
                String.valueOf(command.doctorId()),
                detail.userId(),
                null,
                null);
    }
}
