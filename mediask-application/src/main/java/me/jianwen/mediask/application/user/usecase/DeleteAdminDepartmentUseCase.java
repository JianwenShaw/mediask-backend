package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.user.command.DeleteAdminDepartmentCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.port.AdminDepartmentQueryRepository;
import me.jianwen.mediask.domain.user.port.AdminDepartmentWriteRepository;
import org.springframework.transaction.annotation.Transactional;

public class DeleteAdminDepartmentUseCase {

    private final AdminDepartmentQueryRepository adminDepartmentQueryRepository;
    private final AdminDepartmentWriteRepository adminDepartmentWriteRepository;
    private final AuditTrailService auditTrailService;

    public DeleteAdminDepartmentUseCase(
            AdminDepartmentQueryRepository adminDepartmentQueryRepository,
            AdminDepartmentWriteRepository adminDepartmentWriteRepository,
            AuditTrailService auditTrailService) {
        this.adminDepartmentQueryRepository = adminDepartmentQueryRepository;
        this.adminDepartmentWriteRepository = adminDepartmentWriteRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public void handle(DeleteAdminDepartmentCommand command, AuditContext auditContext) {
        adminDepartmentQueryRepository.findDetailById(command.id())
                .orElseThrow(() -> new BizException(UserErrorCode.ADMIN_DEPARTMENT_NOT_FOUND));
        adminDepartmentWriteRepository.softDelete(command.id());
        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.ADMIN_DEPARTMENT_DELETE,
                AuditResourceTypes.DEPARTMENT,
                String.valueOf(command.id()),
                null,
                null,
                null);
    }
}
