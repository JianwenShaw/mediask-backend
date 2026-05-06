package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.user.usecase.CreateAdminDepartmentUseCase;
import me.jianwen.mediask.application.user.usecase.DeleteAdminDepartmentUseCase;
import me.jianwen.mediask.application.user.usecase.GetAdminDepartmentDetailUseCase;
import me.jianwen.mediask.application.user.usecase.ListAdminDepartmentsUseCase;
import me.jianwen.mediask.application.user.usecase.UpdateAdminDepartmentUseCase;
import me.jianwen.mediask.domain.user.port.AdminDepartmentQueryRepository;
import me.jianwen.mediask.domain.user.port.AdminDepartmentWriteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminDepartmentModuleConfig {

    @Bean
    public ListAdminDepartmentsUseCase listAdminDepartmentsUseCase(
            AdminDepartmentQueryRepository adminDepartmentQueryRepository) {
        return new ListAdminDepartmentsUseCase(adminDepartmentQueryRepository);
    }

    @Bean
    public GetAdminDepartmentDetailUseCase getAdminDepartmentDetailUseCase(
            AdminDepartmentQueryRepository adminDepartmentQueryRepository) {
        return new GetAdminDepartmentDetailUseCase(adminDepartmentQueryRepository);
    }

    @Bean
    public CreateAdminDepartmentUseCase createAdminDepartmentUseCase(
            AdminDepartmentWriteRepository adminDepartmentWriteRepository,
            AuditTrailService auditTrailService) {
        return new CreateAdminDepartmentUseCase(adminDepartmentWriteRepository, auditTrailService);
    }

    @Bean
    public UpdateAdminDepartmentUseCase updateAdminDepartmentUseCase(
            AdminDepartmentWriteRepository adminDepartmentWriteRepository,
            AuditTrailService auditTrailService) {
        return new UpdateAdminDepartmentUseCase(adminDepartmentWriteRepository, auditTrailService);
    }

    @Bean
    public DeleteAdminDepartmentUseCase deleteAdminDepartmentUseCase(
            AdminDepartmentQueryRepository adminDepartmentQueryRepository,
            AdminDepartmentWriteRepository adminDepartmentWriteRepository,
            AuditTrailService auditTrailService) {
        return new DeleteAdminDepartmentUseCase(
                adminDepartmentQueryRepository, adminDepartmentWriteRepository, auditTrailService);
    }
}
