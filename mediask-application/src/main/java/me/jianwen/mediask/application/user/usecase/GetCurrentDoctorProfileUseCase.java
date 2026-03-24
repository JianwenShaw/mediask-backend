package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.user.query.GetCurrentUserQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.DoctorProfile;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.port.DoctorProfileRepository;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.springframework.transaction.annotation.Transactional;

public class GetCurrentDoctorProfileUseCase {

    private final UserAuthenticationRepository userAuthenticationRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    public GetCurrentDoctorProfileUseCase(
            UserAuthenticationRepository userAuthenticationRepository, DoctorProfileRepository doctorProfileRepository) {
        this.userAuthenticationRepository = userAuthenticationRepository;
        this.doctorProfileRepository = doctorProfileRepository;
    }

    @Transactional(readOnly = true)
    public DoctorProfile handle(GetCurrentUserQuery query) {
        AuthenticatedUser authenticatedUser = userAuthenticationRepository.findAuthenticatedUserById(query.userId())
                .orElseThrow(() -> new BizException(UserErrorCode.AUTHENTICATED_USER_NOT_FOUND));
        if (!authenticatedUser.hasRole(RoleCode.DOCTOR)) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
        return doctorProfileRepository.findByUserId(authenticatedUser.userId())
                .orElseThrow(() -> new BizException(UserErrorCode.DOCTOR_PROFILE_NOT_FOUND));
    }
}
