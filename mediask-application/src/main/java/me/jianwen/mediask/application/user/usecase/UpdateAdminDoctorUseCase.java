package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.user.command.UpdateAdminDoctorCommand;
import me.jianwen.mediask.domain.user.model.AdminDoctorDetail;
import me.jianwen.mediask.domain.user.model.AdminDoctorUpdateDraft;
import me.jianwen.mediask.domain.user.port.AdminDoctorWriteRepository;
import org.springframework.transaction.annotation.Transactional;

public class UpdateAdminDoctorUseCase {

    private final AdminDoctorWriteRepository adminDoctorWriteRepository;
    private final AuditTrailService auditTrailService;

    public UpdateAdminDoctorUseCase(
            AdminDoctorWriteRepository adminDoctorWriteRepository,
            AuditTrailService auditTrailService) {
        this.adminDoctorWriteRepository = adminDoctorWriteRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public AdminDoctorDetail handle(UpdateAdminDoctorCommand command, AuditContext auditContext) {
        AdminDoctorDetail detail = adminDoctorWriteRepository.update(
                command.doctorId(),
                new AdminDoctorUpdateDraft(
                        command.displayName(),
                        command.phone(),
                        command.professionalTitle(),
                        command.introductionMasked(),
                        command.departmentIds()));
        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.ADMIN_DOCTOR_UPDATE,
                AuditResourceTypes.DOCTOR_PROFILE,
                String.valueOf(detail.doctorId()),
                detail.userId(),
                null,
                null);
        return detail;
    }
}
