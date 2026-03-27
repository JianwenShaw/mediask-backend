package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.user.command.UpdateCurrentDoctorProfileCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.DoctorProfileDraft;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.port.DoctorProfileWriteRepository;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.springframework.transaction.annotation.Transactional;

public class UpdateCurrentDoctorProfileUseCase {

    private final UserAuthenticationRepository userAuthenticationRepository;
    private final DoctorProfileWriteRepository doctorProfileWriteRepository;

    public UpdateCurrentDoctorProfileUseCase(
            UserAuthenticationRepository userAuthenticationRepository,
            DoctorProfileWriteRepository doctorProfileWriteRepository) {
        this.userAuthenticationRepository = userAuthenticationRepository;
        this.doctorProfileWriteRepository = doctorProfileWriteRepository;
    }

    @Transactional
    public void handle(UpdateCurrentDoctorProfileCommand command) {
        AuthenticatedUser authenticatedUser = userAuthenticationRepository.findAuthenticatedUserById(command.userId())
                .orElseThrow(() -> new BizException(UserErrorCode.AUTHENTICATED_USER_NOT_FOUND));
        if (!authenticatedUser.hasRole(RoleCode.DOCTOR)) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
        doctorProfileWriteRepository.updateByUserId(
                authenticatedUser.userId(),
                new DoctorProfileDraft(command.professionalTitle(), command.introductionMasked()));
    }
}
