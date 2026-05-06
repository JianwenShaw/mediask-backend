package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.user.command.CreateAdminDoctorCommand;
import me.jianwen.mediask.domain.user.model.AdminDoctorCreateDraft;
import me.jianwen.mediask.domain.user.model.AdminDoctorDetail;
import me.jianwen.mediask.domain.user.port.AdminDoctorWriteRepository;
import me.jianwen.mediask.domain.user.port.PasswordHasher;
import org.springframework.transaction.annotation.Transactional;

public class CreateAdminDoctorUseCase {

    private final AdminDoctorWriteRepository adminDoctorWriteRepository;
    private final PasswordHasher passwordHasher;
    private final AuditTrailService auditTrailService;

    public CreateAdminDoctorUseCase(
            AdminDoctorWriteRepository adminDoctorWriteRepository,
            PasswordHasher passwordHasher,
            AuditTrailService auditTrailService) {
        this.adminDoctorWriteRepository = adminDoctorWriteRepository;
        this.passwordHasher = passwordHasher;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public AdminDoctorDetail handle(CreateAdminDoctorCommand command, AuditContext auditContext) {
        AdminDoctorDetail detail = adminDoctorWriteRepository.create(new AdminDoctorCreateDraft(
                command.username(),
                command.phone(),
                passwordHasher.hash(command.password()),
                command.displayName(),
                command.hospitalId(),
                command.professionalTitle(),
                command.introductionMasked(),
                command.departmentIds()));
        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.ADMIN_DOCTOR_CREATE,
                AuditResourceTypes.DOCTOR_PROFILE,
                String.valueOf(detail.doctorId()),
                detail.userId(),
                null,
                null);
        return detail;
    }
}
