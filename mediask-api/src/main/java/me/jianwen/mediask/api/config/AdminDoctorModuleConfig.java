package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.user.usecase.CreateAdminDoctorUseCase;
import me.jianwen.mediask.application.user.usecase.DeleteAdminDoctorUseCase;
import me.jianwen.mediask.application.user.usecase.GetAdminDoctorDetailUseCase;
import me.jianwen.mediask.application.user.usecase.ListAdminDoctorsUseCase;
import me.jianwen.mediask.application.user.usecase.UpdateAdminDoctorUseCase;
import me.jianwen.mediask.domain.user.port.AdminDoctorQueryRepository;
import me.jianwen.mediask.domain.user.port.AdminDoctorWriteRepository;
import me.jianwen.mediask.domain.user.port.PasswordHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminDoctorModuleConfig {

    @Bean
    public ListAdminDoctorsUseCase listAdminDoctorsUseCase(
            AdminDoctorQueryRepository adminDoctorQueryRepository) {
        return new ListAdminDoctorsUseCase(adminDoctorQueryRepository);
    }

    @Bean
    public GetAdminDoctorDetailUseCase getAdminDoctorDetailUseCase(
            AdminDoctorQueryRepository adminDoctorQueryRepository,
            AuditTrailService auditTrailService) {
        return new GetAdminDoctorDetailUseCase(adminDoctorQueryRepository, auditTrailService);
    }

    @Bean
    public CreateAdminDoctorUseCase createAdminDoctorUseCase(
            AdminDoctorWriteRepository adminDoctorWriteRepository,
            PasswordHasher passwordHasher,
            AuditTrailService auditTrailService) {
        return new CreateAdminDoctorUseCase(adminDoctorWriteRepository, passwordHasher, auditTrailService);
    }

    @Bean
    public UpdateAdminDoctorUseCase updateAdminDoctorUseCase(
            AdminDoctorWriteRepository adminDoctorWriteRepository,
            AuditTrailService auditTrailService) {
        return new UpdateAdminDoctorUseCase(adminDoctorWriteRepository, auditTrailService);
    }

    @Bean
    public DeleteAdminDoctorUseCase deleteAdminDoctorUseCase(
            AdminDoctorQueryRepository adminDoctorQueryRepository,
            AdminDoctorWriteRepository adminDoctorWriteRepository,
            AuditTrailService auditTrailService) {
        return new DeleteAdminDoctorUseCase(adminDoctorQueryRepository, adminDoctorWriteRepository, auditTrailService);
    }
}
