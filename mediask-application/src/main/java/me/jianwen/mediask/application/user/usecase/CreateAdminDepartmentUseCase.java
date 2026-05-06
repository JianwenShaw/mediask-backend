package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.user.command.CreateAdminDepartmentCommand;
import me.jianwen.mediask.domain.user.model.AdminDepartmentCreateDraft;
import me.jianwen.mediask.domain.user.model.AdminDepartmentDetail;
import me.jianwen.mediask.domain.user.port.AdminDepartmentWriteRepository;
import org.springframework.transaction.annotation.Transactional;

public class CreateAdminDepartmentUseCase {

    private final AdminDepartmentWriteRepository adminDepartmentWriteRepository;
    private final AuditTrailService auditTrailService;

    public CreateAdminDepartmentUseCase(
            AdminDepartmentWriteRepository adminDepartmentWriteRepository,
            AuditTrailService auditTrailService) {
        this.adminDepartmentWriteRepository = adminDepartmentWriteRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public AdminDepartmentDetail handle(CreateAdminDepartmentCommand command, AuditContext auditContext) {
        AdminDepartmentDetail detail = adminDepartmentWriteRepository.create(new AdminDepartmentCreateDraft(
                command.hospitalId(),
                command.name(),
                command.deptType()));
        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.ADMIN_DEPARTMENT_CREATE,
                AuditResourceTypes.DEPARTMENT,
                String.valueOf(detail.id()),
                null,
                null,
                null);
        return detail;
    }
}
