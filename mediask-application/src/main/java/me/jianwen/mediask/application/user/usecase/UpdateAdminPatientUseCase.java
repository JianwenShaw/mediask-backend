package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.user.command.UpdateAdminPatientCommand;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.model.AdminPatientUpdateDraft;
import me.jianwen.mediask.domain.user.port.AdminPatientWriteRepository;
import org.springframework.transaction.annotation.Transactional;

public class UpdateAdminPatientUseCase {

    private final AdminPatientWriteRepository adminPatientWriteRepository;

    public UpdateAdminPatientUseCase(AdminPatientWriteRepository adminPatientWriteRepository) {
        this.adminPatientWriteRepository = adminPatientWriteRepository;
    }

    @Transactional
    public AdminPatientDetail handle(UpdateAdminPatientCommand command) {
        return adminPatientWriteRepository.update(
                command.patientId(),
                new AdminPatientUpdateDraft(
                        command.displayName(),
                        command.mobileMasked(),
                        command.gender(),
                        command.birthDate(),
                        command.bloodType(),
                        command.allergySummary()));
    }
}
