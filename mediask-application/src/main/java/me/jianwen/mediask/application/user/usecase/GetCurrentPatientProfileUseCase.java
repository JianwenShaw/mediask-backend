package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.user.query.GetCurrentUserQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.PatientProfileSnapshot;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.port.PatientProfileRepository;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.springframework.transaction.annotation.Transactional;

public class GetCurrentPatientProfileUseCase {

    private final UserAuthenticationRepository userAuthenticationRepository;
    private final PatientProfileRepository patientProfileRepository;

    public GetCurrentPatientProfileUseCase(
            UserAuthenticationRepository userAuthenticationRepository, PatientProfileRepository patientProfileRepository) {
        this.userAuthenticationRepository = userAuthenticationRepository;
        this.patientProfileRepository = patientProfileRepository;
    }

    @Transactional(readOnly = true)
    public PatientProfileSnapshot handle(GetCurrentUserQuery query) {
        AuthenticatedUser authenticatedUser = userAuthenticationRepository.findAuthenticatedUserById(query.userId())
                .orElseThrow(() -> new BizException(UserErrorCode.AUTHENTICATED_USER_NOT_FOUND));
        if (!authenticatedUser.hasRole(RoleCode.PATIENT)) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
        return patientProfileRepository.findByUserId(authenticatedUser.userId())
                .orElseThrow(() -> new BizException(UserErrorCode.PATIENT_PROFILE_NOT_FOUND));
    }
}
