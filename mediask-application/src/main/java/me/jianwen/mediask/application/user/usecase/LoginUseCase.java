package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.user.command.LoginCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AccountStatus;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.AuthTokens;
import me.jianwen.mediask.domain.user.model.LoginAccount;
import me.jianwen.mediask.domain.user.model.RefreshTokenSession;
import me.jianwen.mediask.domain.audit.model.AuditActorType;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.PasswordVerifier;
import me.jianwen.mediask.domain.user.port.RefreshTokenManager;
import me.jianwen.mediask.domain.user.port.RefreshTokenStore;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.springframework.transaction.annotation.Transactional;

public class LoginUseCase {

    private final UserAuthenticationRepository userAuthenticationRepository;
    private final PasswordVerifier passwordVerifier;
    private final AccessTokenCodec accessTokenCodec;
    private final RefreshTokenManager refreshTokenManager;
    private final RefreshTokenStore refreshTokenStore;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public LoginUseCase(
            UserAuthenticationRepository userAuthenticationRepository,
            PasswordVerifier passwordVerifier,
            AccessTokenCodec accessTokenCodec,
            RefreshTokenManager refreshTokenManager,
            RefreshTokenStore refreshTokenStore,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.userAuthenticationRepository = userAuthenticationRepository;
        this.passwordVerifier = passwordVerifier;
        this.accessTokenCodec = accessTokenCodec;
        this.refreshTokenManager = refreshTokenManager;
        this.refreshTokenStore = refreshTokenStore;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public AuthenticationResult handle(LoginCommand command, AuditContext auditContext) {
        LoginAccount loginAccount = userAuthenticationRepository.findLoginAccountByUsername(command.username())
                .orElseThrow(() -> new BizException(UserErrorCode.INVALID_CREDENTIALS));
        ensureAccountAvailable(loginAccount.accountStatus());
        ensureRoleAssigned(loginAccount.authenticatedUser());
        if (!passwordVerifier.matches(command.password(), loginAccount.passwordHash())) {
            throw new BizException(UserErrorCode.INVALID_CREDENTIALS);
        }

        AuthenticatedUser authenticatedUser = loginAccount.authenticatedUser();
        userAuthenticationRepository.updateLastLoginAt(authenticatedUser.userId());
        RefreshTokenSession refreshTokenSession = refreshTokenManager.issue(authenticatedUser.userId());
        refreshTokenStore.save(refreshTokenSession);
        AuthenticationResult result = new AuthenticationResult(
                new AuthTokens(accessTokenCodec.issueAccessToken(authenticatedUser, refreshTokenSession.tokenId()), refreshTokenSession),
                authenticatedUser);
        auditTrailService.recordAuditSuccess(
                new AuditContext(
                        auditContext.requestId(),
                        auditContext.traceId(),
                        AuditActorType.USER,
                        authenticatedUser.userId(),
                        authenticatedUser.username(),
                        authenticatedUser.roles().isEmpty() ? null : authenticatedUser.roles().iterator().next().name(),
                        authenticatedUser.primaryDepartmentId(),
                        auditContext.clientIp(),
                        auditContext.userAgent(),
                        auditContext.occurredAt()),
                AuditActionCodes.AUTH_LOGIN_SUCCESS,
                AuditResourceTypes.AUTH_SESSION,
                String.valueOf(authenticatedUser.userId()),
                null,
                null,
                null);
        return result;
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
