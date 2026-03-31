package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.user.command.CreateAdminPatientCommand;
import me.jianwen.mediask.domain.user.model.AdminPatientCreateDraft;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.port.AdminPatientWriteRepository;
import me.jianwen.mediask.domain.user.port.PasswordHasher;
import org.springframework.transaction.annotation.Transactional;

public class CreateAdminPatientUseCase {

    private final AdminPatientWriteRepository adminPatientWriteRepository;
    private final PasswordHasher passwordHasher;

    public CreateAdminPatientUseCase(
            AdminPatientWriteRepository adminPatientWriteRepository, PasswordHasher passwordHasher) {
        this.adminPatientWriteRepository = adminPatientWriteRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public AdminPatientDetail handle(CreateAdminPatientCommand command) {
        return adminPatientWriteRepository.create(new AdminPatientCreateDraft(
                command.username(),
                passwordHasher.hash(command.password()),
                command.displayName(),
                command.mobileMasked(),
                command.gender(),
                command.birthDate(),
                command.bloodType(),
                command.allergySummary()));
    }
}
