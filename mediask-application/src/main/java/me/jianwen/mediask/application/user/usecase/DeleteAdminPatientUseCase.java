package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.user.command.DeleteAdminPatientCommand;
import me.jianwen.mediask.domain.user.port.AdminPatientWriteRepository;
import org.springframework.transaction.annotation.Transactional;

public class DeleteAdminPatientUseCase {

    private final AdminPatientWriteRepository adminPatientWriteRepository;

    public DeleteAdminPatientUseCase(AdminPatientWriteRepository adminPatientWriteRepository) {
        this.adminPatientWriteRepository = adminPatientWriteRepository;
    }

    @Transactional
    public void handle(DeleteAdminPatientCommand command) {
        adminPatientWriteRepository.softDelete(command.patientId());
    }
}
