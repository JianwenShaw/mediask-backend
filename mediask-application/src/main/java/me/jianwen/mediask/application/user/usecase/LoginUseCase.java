package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.user.command.LoginCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AccountStatus;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.LoginAccount;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.PasswordVerifier;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.springframework.transaction.annotation.Transactional;

public class LoginUseCase {

    private final UserAuthenticationRepository userAuthenticationRepository;
    private final PasswordVerifier passwordVerifier;
    private final AccessTokenCodec accessTokenCodec;

    public LoginUseCase(
            UserAuthenticationRepository userAuthenticationRepository,
            PasswordVerifier passwordVerifier,
            AccessTokenCodec accessTokenCodec) {
        this.userAuthenticationRepository = userAuthenticationRepository;
        this.passwordVerifier = passwordVerifier;
        this.accessTokenCodec = accessTokenCodec;
    }

    @Transactional
    public LoginResult handle(LoginCommand command) {
        LoginAccount loginAccount = userAuthenticationRepository.findLoginAccountByUsername(command.username())
                .orElseThrow(() -> new BizException(UserErrorCode.INVALID_CREDENTIALS));
        ensureAccountAvailable(loginAccount.accountStatus());
        ensureRoleAssigned(loginAccount.authenticatedUser());
        if (!passwordVerifier.matches(command.password(), loginAccount.passwordHash())) {
            throw new BizException(UserErrorCode.INVALID_CREDENTIALS);
        }

        AuthenticatedUser authenticatedUser = loginAccount.authenticatedUser();
        userAuthenticationRepository.updateLastLoginAt(authenticatedUser.userId());
        return new LoginResult(accessTokenCodec.issueAccessToken(authenticatedUser), authenticatedUser);
    }

    private void ensureAccountAvailable(AccountStatus accountStatus) {
        if (accountStatus == AccountStatus.DISABLED) {
            throw new BizException(UserErrorCode.ACCOUNT_DISABLED);
        }
        if (accountStatus == AccountStatus.LOCKED) {
            throw new BizException(UserErrorCode.ACCOUNT_LOCKED);
        }
    }

    private void ensureRoleAssigned(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser.roles().isEmpty()) {
            throw new BizException(UserErrorCode.ROLE_NOT_ASSIGNED);
        }
    }
}
