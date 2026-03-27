package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.user.command.UpdateCurrentPatientProfileCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.PatientProfileDraft;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.port.PatientProfileWriteRepository;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.springframework.transaction.annotation.Transactional;

public class UpdateCurrentPatientProfileUseCase {

    private final UserAuthenticationRepository userAuthenticationRepository;
    private final PatientProfileWriteRepository patientProfileWriteRepository;

    public UpdateCurrentPatientProfileUseCase(
            UserAuthenticationRepository userAuthenticationRepository,
            PatientProfileWriteRepository patientProfileWriteRepository) {
        this.userAuthenticationRepository = userAuthenticationRepository;
        this.patientProfileWriteRepository = patientProfileWriteRepository;
    }

    @Transactional
    public void handle(UpdateCurrentPatientProfileCommand command) {
        AuthenticatedUser authenticatedUser = userAuthenticationRepository.findAuthenticatedUserById(command.userId())
                .orElseThrow(() -> new BizException(UserErrorCode.AUTHENTICATED_USER_NOT_FOUND));
        if (!authenticatedUser.hasRole(RoleCode.PATIENT)) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
        patientProfileWriteRepository.updateByUserId(
                authenticatedUser.userId(),
                new PatientProfileDraft(
                        command.gender(),
                        command.birthDate(),
                        command.bloodType(),
                        command.allergySummary()));
    }
}
