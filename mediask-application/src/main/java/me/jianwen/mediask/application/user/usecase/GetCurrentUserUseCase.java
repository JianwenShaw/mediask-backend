package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.user.query.GetCurrentUserQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.springframework.transaction.annotation.Transactional;

public class GetCurrentUserUseCase {

    private final UserAuthenticationRepository userAuthenticationRepository;

    public GetCurrentUserUseCase(UserAuthenticationRepository userAuthenticationRepository) {
        this.userAuthenticationRepository = userAuthenticationRepository;
    }

    @Transactional(readOnly = true)
    public AuthenticatedUser handle(GetCurrentUserQuery query) {
        AuthenticatedUser authenticatedUser = userAuthenticationRepository.findAuthenticatedUserById(query.userId())
                .orElseThrow(() -> new BizException(UserErrorCode.AUTHENTICATED_USER_NOT_FOUND));
        ensureRoleAssigned(authenticatedUser);
        return authenticatedUser;
    }

    private void ensureRoleAssigned(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser.roles().isEmpty()) {
            throw new BizException(UserErrorCode.ROLE_NOT_ASSIGNED);
        }
    }
}
