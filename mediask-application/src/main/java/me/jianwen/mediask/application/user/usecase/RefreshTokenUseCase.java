package me.jianwen.mediask.application.user.usecase;

import java.time.Clock;
import me.jianwen.mediask.application.user.command.RefreshTokenCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AuthTokens;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.RefreshTokenSession;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.RefreshTokenManager;
import me.jianwen.mediask.domain.user.port.RefreshTokenStore;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.springframework.transaction.annotation.Transactional;

public class RefreshTokenUseCase {

    private final UserAuthenticationRepository userAuthenticationRepository;
    private final AccessTokenCodec accessTokenCodec;
    private final RefreshTokenManager refreshTokenManager;
    private final RefreshTokenStore refreshTokenStore;
    private final Clock clock;

    public RefreshTokenUseCase(
            UserAuthenticationRepository userAuthenticationRepository,
            AccessTokenCodec accessTokenCodec,
            RefreshTokenManager refreshTokenManager,
            RefreshTokenStore refreshTokenStore,
            Clock clock) {
        this.userAuthenticationRepository = userAuthenticationRepository;
        this.accessTokenCodec = accessTokenCodec;
        this.refreshTokenManager = refreshTokenManager;
        this.refreshTokenStore = refreshTokenStore;
        this.clock = clock;
    }

    @Transactional
    public AuthenticationResult handle(RefreshTokenCommand command) {
        RefreshTokenSession refreshTokenSession = refreshTokenStore.findByTokenValue(command.refreshToken())
                .orElseThrow(() -> new BizException(UserErrorCode.INVALID_REFRESH_TOKEN));
        if (refreshTokenSession.isExpiredAt(clock.instant())) {
            refreshTokenStore.deleteByTokenValue(command.refreshToken());
            throw new BizException(UserErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        AuthenticatedUser authenticatedUser = userAuthenticationRepository
                .findAuthenticatedUserById(refreshTokenSession.userId())
                .orElseThrow(() -> new BizException(UserErrorCode.AUTHENTICATED_USER_NOT_FOUND));
        ensureRoleAssigned(authenticatedUser);
        ensureRefreshPermissionAssigned(authenticatedUser);

        RefreshTokenSession nextRefreshTokenSession = refreshTokenManager.issue(authenticatedUser.userId());
        if (!refreshTokenStore.rotate(command.refreshToken(), nextRefreshTokenSession)) {
            throw new BizException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }
        return new AuthenticationResult(
                new AuthTokens(
                        accessTokenCodec.issueAccessToken(authenticatedUser, nextRefreshTokenSession.tokenId()),
                        nextRefreshTokenSession),
                authenticatedUser);
    }

    private void ensureRoleAssigned(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser.roles().isEmpty()) {
            throw new BizException(UserErrorCode.ROLE_NOT_ASSIGNED);
        }
    }

    private void ensureRefreshPermissionAssigned(AuthenticatedUser authenticatedUser) {
        if (!authenticatedUser.hasPermission("auth:refresh")) {
            throw new BizException(UserErrorCode.PERMISSION_DENIED);
        }
    }
}
