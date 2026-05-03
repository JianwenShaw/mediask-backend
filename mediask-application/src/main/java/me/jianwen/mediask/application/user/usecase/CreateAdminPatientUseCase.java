package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.user.command.CreateAdminPatientCommand;
import me.jianwen.mediask.domain.user.model.AdminPatientCreateDraft;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.port.AdminPatientWriteRepository;
import me.jianwen.mediask.domain.user.port.PasswordHasher;
import org.springframework.transaction.annotation.Transactional;

public class CreateAdminPatientUseCase {

    private final AdminPatientWriteRepository adminPatientWriteRepository;
    private final PasswordHasher passwordHasher;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public CreateAdminPatientUseCase(
            AdminPatientWriteRepository adminPatientWriteRepository,
            PasswordHasher passwordHasher,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.adminPatientWriteRepository = adminPatientWriteRepository;
        this.passwordHasher = passwordHasher;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public AdminPatientDetail handle(CreateAdminPatientCommand command, AuditContext auditContext) {
        AdminPatientDetail detail = adminPatientWriteRepository.create(new AdminPatientCreateDraft(
                command.username(),
                passwordHasher.hash(command.password()),
                command.displayName(),
                command.mobileMasked(),
                command.gender(),
                command.birthDate(),
                command.bloodType(),
                command.allergySummary()));
        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.ADMIN_PATIENT_CREATE,
                AuditResourceTypes.PATIENT_PROFILE,
                String.valueOf(detail.patientId()),
                detail.userId(),
                null,
                null);
        return detail;
    }
}
